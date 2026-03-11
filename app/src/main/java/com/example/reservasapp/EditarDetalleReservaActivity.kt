package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarDetalleReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RESERVA_ID = "extra_reserva_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_detalle_reserva)

        val reservaId = intent.getStringExtra(EXTRA_RESERVA_ID).orEmpty()
        val reserva = ReservasRepository.obtenerReservaPorId(reservaId)

        if (reserva == null) {
            finish()
            return
        }

        val tvFecha = findViewById<TextView>(R.id.tvFechaEditar)
        val container = findViewById<LinearLayout>(R.id.containerSeccionesEditar)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarEdicion)

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(reserva.fechaMillis))
        tvFecha.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        val spinnersPorSeccion = linkedMapOf<String, Spinner>()

        fun renderSecciones(secciones: List<MenuSection>) {
            container.removeAllViews()
            spinnersPorSeccion.clear()

            secciones.forEach { section ->
                val title = TextView(this).apply {
                    text = section.nombre
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 20 }
                }

                val opciones = section.opciones.map { it.nombre }
                val spinner = Spinner(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    adapter = ArrayAdapter(
                        this@EditarDetalleReservaActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        opciones
                    )
                }

                val actual = reserva.selecciones[section.nombre]
                val index = opciones.indexOf(actual).coerceAtLeast(0)
                spinner.setSelection(index)

                container.addView(title)
                container.addView(spinner)
                spinnersPorSeccion[section.nombre] = spinner
            }
        }

        MenuRepository.cargarSecciones { ok, secciones ->
            runOnUiThread {
                renderSecciones(secciones)
                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_menu, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnConfirmar.setOnClickListener {
            val selecciones = spinnersPorSeccion.mapValues { (_, spinner) ->
                spinner.selectedItem?.toString().orEmpty()
            }

            ReservasRepository.actualizarReserva(
                id = reserva.id,
                selecciones = selecciones
            ) { actualizada ->
                if (actualizada == null) {
                    Toast.makeText(this, R.string.error_actualizar_reserva, Toast.LENGTH_LONG).show()
                    return@actualizarReserva
                }

                val resumen = ReservasRepository.formatearSelecciones(actualizada.selecciones)
                val intent = Intent(this, ConfirmacionReservaActivity::class.java).apply {
                    putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, fechaFormateada)
                    putExtra(ConfirmacionReservaActivity.EXTRA_DETALLE, resumen)
                    putExtra(ConfirmacionReservaActivity.EXTRA_RESERVA_ID, actualizada.id)
                    putExtra(ConfirmacionReservaActivity.EXTRA_ES_EDICION, true)
                }
                startActivity(intent)
            }
        }
    }
}
