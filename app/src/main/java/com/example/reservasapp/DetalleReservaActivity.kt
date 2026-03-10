package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetalleReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE_MILLIS = "extra_date_millis"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva)

        val dateText = findViewById<TextView>(R.id.tvFechaSeleccionada)
        val tabLayout = findViewById<TabLayout>(R.id.tabSections)
        val recycler = findViewById<RecyclerView>(R.id.recyclerMenuOptions)
        val btnContinuar = findViewById<Button>(R.id.btnConfirmar)

        val selectedDateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, System.currentTimeMillis())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(selectedDateMillis))
        dateText.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        var secciones = MenuRepository.obtenerSeccionesCache()
        val selecciones = linkedMapOf<String, String>()
        var currentSectionIndex = 0

        val adapter = MenuOptionAdapter(emptyList()) { option ->
            val section = secciones.getOrNull(currentSectionIndex) ?: return@MenuOptionAdapter
            selecciones[section.nombre] = option.name
            btnContinuar.isEnabled = true
            btnContinuar.alpha = 1f
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        secciones.forEach { section ->
            tabLayout.addTab(tabLayout.newTab().setText(section.nombre))
        }

        fun showSection(position: Int) {
            val section = secciones.getOrNull(position) ?: return
            currentSectionIndex = position
            val items = MenuVisualRepository.buildItemsForSection(section.opciones)
            adapter.updateItems(items, selecciones[section.nombre])
            val isSelected = selecciones.containsKey(section.nombre)
            btnContinuar.isEnabled = isSelected
            btnContinuar.alpha = if (isSelected) 1f else 0.5f
            btnContinuar.text = getString(R.string.continuar)
        }

        MenuRepository.cargarSecciones { ok, loadedSections ->
            runOnUiThread {
                secciones = loadedSections
                tabLayout.removeAllTabs()
                secciones.forEach { section ->
                    tabLayout.addTab(tabLayout.newTab().setText(section.nombre))
                }

                if (secciones.isNotEmpty()) {
                    currentSectionIndex = 0
                    showSection(0)
                }

                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                }
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showSection(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        btnContinuar.setOnClickListener {
            val section = secciones.getOrNull(currentSectionIndex) ?: return@setOnClickListener
            if (!selecciones.containsKey(section.nombre)) return@setOnClickListener

            if (currentSectionIndex < secciones.lastIndex) {
                currentSectionIndex += 1
                tabLayout.getTabAt(currentSectionIndex)?.select()
            } else {
                ReservasRepository.agregarReserva(
                    fechaMillis = selectedDateMillis,
                    selecciones = selecciones
                ) { reserva ->
                    if (reserva == null) {
                        Toast.makeText(this, R.string.error_guardar_reserva, Toast.LENGTH_LONG).show()
                        return@agregarReserva
                    }

                    val resumen = ReservasRepository.formatearSelecciones(reserva.selecciones)
                    val intent = Intent(this, ConfirmacionReservaActivity::class.java).apply {
                        putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, fechaFormateada)
                        putExtra(ConfirmacionReservaActivity.EXTRA_DETALLE, resumen)
                        putExtra(ConfirmacionReservaActivity.EXTRA_RESERVA_ID, reserva.id)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}
