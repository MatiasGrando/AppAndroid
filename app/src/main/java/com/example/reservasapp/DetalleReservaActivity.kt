package com.example.reservasapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DetalleReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DATE = "extra_date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_reserva)

        val dateText = findViewById<TextView>(R.id.tvFechaSeleccionada)
        val comidaSpinner = findViewById<Spinner>(R.id.spinnerComida)
        val postreSpinner = findViewById<Spinner>(R.id.spinnerPostre)
        val confirmarButton = findViewById<Button>(R.id.btnConfirmar)

        val selectedDate = intent.getStringExtra(EXTRA_DATE).orEmpty()
        dateText.text = getString(R.string.fecha_seleccionada, selectedDate)

        comidaSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Pollo", "Carne")
        )

        postreSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Helado", "Alfajor")
        )

        confirmarButton.setOnClickListener {
            val resumen = getString(
                R.string.resumen_reserva,
                selectedDate,
                comidaSpinner.selectedItem.toString(),
                postreSpinner.selectedItem.toString()
            )
            Toast.makeText(this, resumen, Toast.LENGTH_LONG).show()
        }
    }
}
