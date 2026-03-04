package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfirmacionReservaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_DETALLE = "extra_detalle"
        const val EXTRA_RESERVA_ID = "extra_reserva_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmacion_reserva)

        val fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty()
        val detalleSeleccion = intent.getStringExtra(EXTRA_DETALLE).orEmpty()
        val reservaId = intent.getLongExtra(EXTRA_RESERVA_ID, -1L)
        val reserva = ReservasRepository.obtenerReservaPorId(reservaId)

        val titulo = findViewById<TextView>(R.id.tvTituloConfirmacion)
        val detalle = findViewById<TextView>(R.id.tvDetalleConfirmacion)
        val tvFechaReserva = findViewById<TextView>(R.id.tvFechaReserva)
        val tvPrincipal = findViewById<TextView>(R.id.tvPlatoPrincipalNombre)
        val tvGuarnicion = findViewById<TextView>(R.id.tvGuarnicionNombre)
        val tvPostre = findViewById<TextView>(R.id.tvPostreNombre)
        val ivPrincipal = findViewById<ImageView>(R.id.ivPlatoPrincipal)
        val ivGuarnicion = findViewById<ImageView>(R.id.ivGuarnicion)
        val ivPostre = findViewById<ImageView>(R.id.ivPostre)
        val volverMenu = findViewById<Button>(R.id.btnVolverMenu)

        titulo.text = getString(R.string.titulo_confirmacion)

        if (reserva != null) {
            val principal = extraerSeleccion(reserva.selecciones, "plato", "principal")
            val guarnicion = extraerSeleccion(reserva.selecciones, "guarn")
            val postre = extraerSeleccion(reserva.selecciones, "postre")

            val formatter = SimpleDateFormat("EEEE d/M/yy", Locale("es", "ES"))
            tvFechaReserva.text = formatter.format(Date(reserva.fechaMillis)).uppercase(Locale("es", "ES"))
            tvPrincipal.text = principal ?: "-"
            tvGuarnicion.text = guarnicion ?: "-"
            tvPostre.text = postre ?: "-"

            ivPrincipal.setImageResource(MenuVisualRepository.imageForSelection("Plato principal", principal))
            ivGuarnicion.setImageResource(MenuVisualRepository.imageForSelection("Guarnición", guarnicion))
            ivPostre.setImageResource(MenuVisualRepository.imageForSelection("Postre", postre))

            detalle.text = getString(R.string.resumen_confirmacion_fecha, fecha)
        } else {
            detalle.text = getString(R.string.resumen_reserva_generico, fecha, detalleSeleccion)
        }

        volverMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun extraerSeleccion(selecciones: Map<String, String>, vararg aliases: String): String? {
        return selecciones.entries.firstOrNull { (key, _) ->
            val keyNormalized = key.lowercase()
            aliases.all { keyNormalized.contains(it.lowercase()) }
        }?.value
    }
}
