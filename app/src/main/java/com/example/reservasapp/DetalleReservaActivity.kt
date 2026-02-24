package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
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
        val comidaSpinner = findViewById<Spinner>(R.id.spinnerComida)
        val postreSpinner = findViewById<Spinner>(R.id.spinnerPostre)
        val confirmarButton = findViewById<Button>(R.id.btnConfirmar)

        val selectedDateMillis = intent.getLongExtra(EXTRA_DATE_MILLIS, System.currentTimeMillis())
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(selectedDateMillis))
        dateText.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        val opcionesComida = MenuRepository.obtenerOpcionesPorSeccion("Comida principal").ifEmpty { listOf("Pollo", "Carne") }
        val opcionesPostre = MenuRepository.obtenerOpcionesPorSeccion("Postre").ifEmpty { listOf("Helado", "Alfajor") }

        comidaSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcionesComida
        )

        postreSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            opcionesPostre
        )

        confirmarButton.setOnClickListener {
            val reserva = ReservasRepository.agregarReserva(
                fechaMillis = selectedDateMillis,
                comida = comidaSpinner.selectedItem.toString(),
                postre = postreSpinner.selectedItem.toString()
            )

            val intent = Intent(this, ConfirmacionReservaActivity::class.java).apply {
                putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, fechaFormateada)
                putExtra(ConfirmacionReservaActivity.EXTRA_COMIDA, reserva.comida)
                putExtra(ConfirmacionReservaActivity.EXTRA_POSTRE, reserva.postre)
            }
            startActivity(intent)
        }
    }
}
