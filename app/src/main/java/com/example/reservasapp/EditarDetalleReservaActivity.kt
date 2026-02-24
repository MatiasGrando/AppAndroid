package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
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

        val reservaId = intent.getLongExtra(EXTRA_RESERVA_ID, -1L)
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

        val secciones = MenuRepository.obtenerSecciones()
        val spinnersPorSeccion = linkedMapOf<String, Spinner>()

        secciones.forEach { section ->
            val title = TextView(this).apply {
                text = section.nombre
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 20 }
            }

            val spinner = Spinner(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adapter = ArrayAdapter(
                    this@EditarDetalleReservaActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    section.opciones
                )
            }

            val actual = reserva.selecciones[section.nombre]
            val index = section.opciones.indexOf(actual).coerceAtLeast(0)
            spinner.setSelection(index)

            container.addView(title)
            container.addView(spinner)
            spinnersPorSeccion[section.nombre] = spinner
        }

        btnConfirmar.setOnClickListener {
            val selecciones = spinnersPorSeccion.mapValues { (_, spinner) ->
                spinner.selectedItem?.toString().orEmpty()
            }

            val actualizada = ReservasRepository.actualizarReserva(
                id = reserva.id,
                selecciones = selecciones
            ) ?: return@setOnClickListener

            val resumen = ReservasRepository.formatearSelecciones(actualizada.selecciones)

            val intent = Intent(this, ConfirmacionEdicionActivity::class.java).apply {
                putExtra(ConfirmacionEdicionActivity.EXTRA_FECHA, fechaFormateada)
                putExtra(ConfirmacionEdicionActivity.EXTRA_DETALLE, resumen)
            }
            startActivity(intent)
        }
    }
}
