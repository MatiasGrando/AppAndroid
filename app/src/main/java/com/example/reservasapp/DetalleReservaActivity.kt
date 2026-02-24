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

class DetalleReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE_MILLIS = "extra_date_millis"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva)

        val dateText = findViewById<TextView>(R.id.tvFechaSeleccionada)
        val container = findViewById<LinearLayout>(R.id.containerSecciones)
        val confirmarButton = findViewById<Button>(R.id.btnConfirmar)

        val selectedDateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, System.currentTimeMillis())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(selectedDateMillis))
        dateText.text = getString(R.string.fecha_seleccionada, fechaFormateada)

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
                    this@DetalleReservaActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    section.opciones
                )
            }

            container.addView(title)
            container.addView(spinner)
            spinnersPorSeccion[section.nombre] = spinner
        }

        confirmarButton.setOnClickListener {
            val selecciones = spinnersPorSeccion.mapValues { (_, spinner) ->
                spinner.selectedItem?.toString().orEmpty()
            }

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
