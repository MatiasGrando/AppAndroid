package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
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

        val secciones = MenuRepository.obtenerSecciones()
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
            val items = MenuVisualRepository.buildItemsForSection(section.nombre, section.opciones)
            adapter.updateItems(items, selecciones[section.nombre])
            val isSelected = selecciones.containsKey(section.nombre)
            btnContinuar.isEnabled = isSelected
            btnContinuar.alpha = if (isSelected) 1f else 0.5f
            btnContinuar.text = getString(R.string.continuar)
        }

        if (secciones.isNotEmpty()) {
            showSection(0)
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
                val reserva = ReservasRepository.agregarReserva(
                    fechaMillis = selectedDateMillis,
                    selecciones = selecciones
                )

                val resumen = ReservasRepository.formatearSelecciones(reserva.selecciones)

                val intent = Intent(this, ConfirmacionReservaActivity::class.java).apply {
                    putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, fechaFormateada)
                    putExtra(ConfirmacionReservaActivity.EXTRA_DETALLE, resumen)
                }
                startActivity(intent)
            }
        }
    }
}
