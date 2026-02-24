package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfirmacionReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_DETALLE = "extra_detalle"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_reserva)

        val fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty()
        val detalleSeleccion = intent.getStringExtra(EXTRA_DETALLE).orEmpty()

        val detalle = findViewById<TextView>(R.id.tvDetalleConfirmacion)
        val volverMenu = findViewById<Button>(R.id.btnVolverMenu)

        detalle.text = getString(R.string.resumen_reserva_generico, fecha, detalleSeleccion)

        volverMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
