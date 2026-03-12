package com.example.reservasapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminMenuFechaActivity : AppCompatActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedConfigDateMillis: Long = Calendar.getInstance().clearTime().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu_fecha)

        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaConfigSeleccionada)
        val btnSeleccionarFecha = findViewById<Button>(R.id.btnSeleccionarFechaConfig)
        val btnGuardarFecha = findViewById<Button>(R.id.btnGuardarPlatosPorFecha)
        val listPlatosFecha = findViewById<ListView>(R.id.listPlatosPorFecha)

        var platosConfigurables: List<DishConfigOption> = emptyList()

        fun actualizarTextoFecha() {
            tvFechaSeleccionada.text = getString(
                R.string.admin_menu_fecha_config,
                dateFormatter.format(Date(selectedConfigDateMillis))
            )
        }

        fun cargarOpcionesPorFecha() {
            MenuRepository.cargarSecciones { okMenu, loadedSections ->
                if (!okMenu) {
                    runOnUiThread {
                        Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                    }
                    return@cargarSecciones
                }

                MenuRepository.obtenerClavesPlatosHabilitados(selectedConfigDateMillis) { okConfig, dishKeys ->
                    runOnUiThread {
                        if (!okConfig) {
                            Toast.makeText(this, R.string.error_cargar_menu_fecha, Toast.LENGTH_SHORT).show()
                            return@runOnUiThread
                        }

                        platosConfigurables = loadedSections.flatMap { section ->
                            section.opciones.map { dish ->
                                DishConfigOption(
                                    label = "${section.nombre} • ${dish.nombre}",
                                    dishKey = MenuRepository.clavePlato(section.nombre, dish.nombre)
                                )
                            }
                        }

                        val labels = platosConfigurables.map { it.label }
                        listPlatosFecha.choiceMode = ListView.CHOICE_MODE_MULTIPLE
                        listPlatosFecha.adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_multiple_choice,
                            labels
                        )

                        val useFallback = dishKeys == null
                        platosConfigurables.forEachIndexed { index, option ->
                            val checked = if (useFallback) true else dishKeys.contains(option.dishKey)
                            listPlatosFecha.setItemChecked(index, checked)
                        }
                    }
                }
            }
        }

        actualizarTextoFecha()
        cargarOpcionesPorFecha()

        btnSeleccionarFecha.setOnClickListener {
            val calendar = Calendar.getInstance().clearTime().apply { timeInMillis = selectedConfigDateMillis }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance().clearTime().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedConfigDateMillis = selected.timeInMillis
                    actualizarTextoFecha()
                    cargarOpcionesPorFecha()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnGuardarFecha.setOnClickListener {
            if (platosConfigurables.isEmpty()) {
                Toast.makeText(this, R.string.error_sin_platos_para_configurar, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedKeys = mutableSetOf<String>()
            for (i in platosConfigurables.indices) {
                if (listPlatosFecha.isItemChecked(i)) {
                    selectedKeys.add(platosConfigurables[i].dishKey)
                }
            }

            MenuRepository.guardarPlatosHabilitadosParaFecha(selectedConfigDateMillis, selectedKeys) { ok ->
                runOnUiThread {
                    if (!ok) {
                        Toast.makeText(this, R.string.error_guardar_menu_fecha, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    Toast.makeText(this, R.string.mensaje_menu_fecha_guardado, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private data class DishConfigOption(
    val label: String,
    val dishKey: String
)
