package com.example.reservasapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetallePedidosUsuariosActivity : BaseActivity() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", spanishDateLocale)
    private var selectedDateMillis: Long = Calendar.getInstance().clearTime().timeInMillis

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_detalle_pedidos_usuarios)

        val tvFechaSeleccionada = findViewById<TextView>(R.id.tvFechaDetallePedidosSeleccionada)
        val btnSeleccionarFecha = findViewById<Button>(R.id.btnSeleccionarFechaDetallePedidos)
        val btnGenerarDetalle = findViewById<Button>(R.id.btnGenerarDetallePedidos)
        val tvDetalle = findViewById<TextView>(R.id.tvDetallePedidosUsuarios)

        fun actualizarTextoFecha() {
            tvFechaSeleccionada.text = getString(
                R.string.pedidos_por_dia_fecha,
                dateFormatter.format(Date(selectedDateMillis))
            )
        }

        fun renderDetalle(rows: List<ReservasRepository.DetallePedidoUsuario>) {
            if (rows.isEmpty()) {
                tvDetalle.text = getString(R.string.detalle_pedidos_usuarios_sin_datos)
                return
            }

            val fechaTitulo = SimpleDateFormat("EEEE dd/MM", Locale("es", "ES"))
                .format(Date(selectedDateMillis))
                .uppercase(Locale("es", "ES"))

            val lines = mutableListOf(fechaTitulo)
            var empresaActual = ""

            rows.forEach { row ->
                if (row.empresa != empresaActual) {
                    empresaActual = row.empresa
                    lines += ""
                    lines += getString(R.string.detalle_pedidos_empresa, empresaActual)
                }

                val nombreCompleto = listOf(row.nombre, row.apellido)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { getString(R.string.detalle_pedidos_usuario_sin_nombre) }

                val guarnicion = row.guarnicion.trim()
                val tieneGuarnicion = guarnicion.isNotEmpty() && guarnicion != "-"

                lines += if (tieneGuarnicion) {
                    String.format(
                        Locale.getDefault(),
                        "%-18s %-24s %-16s %s",
                        nombreCompleto,
                        row.platoPrincipal,
                        guarnicion,
                        row.postre
                    )
                } else {
                    String.format(
                        Locale.getDefault(),
                        "%-18s %-24s %s",
                        nombreCompleto,
                        row.platoPrincipal,
                        row.postre
                    )
                }
            }

            tvDetalle.text = lines.joinToString("\n")
        }

        actualizarTextoFecha()

        btnSeleccionarFecha.setOnClickListener {
            createSpanishDatePickerDialog(selectedDateMillis) { selectedMillis ->
                selectedDateMillis = selectedMillis
                actualizarTextoFecha()
            }.show()
        }

        btnGenerarDetalle.setOnClickListener {
            tvDetalle.text = getString(R.string.pedidos_por_dia_cargando)
            ReservasRepository.obtenerDetallePedidosPorFecha(selectedDateMillis) { ok, detalle ->
                runOnUiThread {
                    if (!ok) {
                        tvDetalle.text = getString(R.string.detalle_pedidos_usuarios_sin_datos)
                        Toast.makeText(
                            this,
                            R.string.error_cargar_detalle_pedidos_usuarios,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@runOnUiThread
                    }
                    renderDetalle(detalle)
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
