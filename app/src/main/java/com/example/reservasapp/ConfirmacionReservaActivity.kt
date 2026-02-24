package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfirmacionReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_COMIDA = "extra_comida"
        const val EXTRA_POSTRE = "extra_postre"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_reserva)

        val fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty()
        val comida = intent.getStringExtra(EXTRA_COMIDA).orEmpty()
        val postre = intent.getStringExtra(EXTRA_POSTRE).orEmpty()

        val detalle = findViewById<TextView>(R.id.tvDetalleConfirmacion)
        val volverMenu = findViewById<Button>(R.id.btnVolverMenu)

        detalle.text = getString(R.string.resumen_reserva, fecha, comida, postre)

        volverMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
