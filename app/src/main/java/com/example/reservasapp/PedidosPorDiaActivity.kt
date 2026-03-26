package com.example.reservasapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PedidosPorDiaActivity : BaseActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDateMillis: Long = Calendar.getInstance().clearTime().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_pedidos_por_dia)

        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaPedidosSeleccionada)
        val btnSeleccionarFecha = findViewById<Button>(R.id.btnSeleccionarFechaPedidos)
        val btnGenerarResumen = findViewById<Button>(R.id.btnGenerarResumenPedidos)
        val tvResumen = findViewById<TextView>(R.id.tvResumenPedidos)

        fun actualizarTextoFecha() {
            tvFechaSeleccionada.text = getString(
                R.string.pedidos_por_dia_fecha,
                dateFormatter.format(Date(selectedDateMillis))
            )
        }

        fun renderResumen(groupedCounts: Map<String, Map<String, Int>>) {
            val preferredOrder = listOf(
                "Plato principal",
                "Guarniciones",
                "Postres"
            )

            val sectionOrder = preferredOrder + groupedCounts.keys.filterNot { it in preferredOrder }.sorted()
            val lines = mutableListOf<String>()

            var isFirstSection = true
            sectionOrder.forEach { section ->
                val dishes = groupedCounts[section].orEmpty()
                if (dishes.isEmpty()) return@forEach

                if (isFirstSection) {
                    lines += getString(R.string.pedidos_por_dia_header)
                    isFirstSection = false
                } else {
                    lines += ""
                    lines += section
                }

                dishes.toList()
                    .sortedBy { it.first }
                    .forEach { (dishName, count) ->
                        lines += String.format(Locale.getDefault(), "%-30s %d", dishName, count)
                    }
            }

            tvResumen.text = if (lines.isEmpty()) {
                getString(R.string.pedidos_por_dia_sin_datos)
            } else {
                lines.joinToString("\n")
            }
        }

        actualizarTextoFecha()

        btnSeleccionarFecha.setOnClickListener {
            val calendar = Calendar.getInstance().clearTime().apply { timeInMillis = selectedDateMillis }
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDateMillis = Calendar.getInstance().clearTime().apply {
                        set(year, month, dayOfMonth)
                    }.timeInMillis
                    actualizarTextoFecha()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnGenerarResumen.setOnClickListener {
            tvResumen.text = getString(R.string.pedidos_por_dia_cargando)
            ReservasRepository.obtenerResumenPedidosPorFecha(selectedDateMillis) { ok, groupedCounts ->
                runOnUiThread {
                    if (!ok) {
                        tvResumen.text = getString(R.string.pedidos_por_dia_sin_datos)
                        Toast.makeText(this, R.string.error_cargar_pedidos_por_dia, Toast.LENGTH_SHORT)
                            .show()
                        return@runOnUiThread
                    }
                    renderResumen(groupedCounts)
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
