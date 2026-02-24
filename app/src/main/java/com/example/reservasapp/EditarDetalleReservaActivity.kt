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
        val spinnerComida = findViewById<Spinner>(R.id.spinnerComidaEditar)
        val spinnerPostre = findViewById<Spinner>(R.id.spinnerPostreEditar)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmarEdicion)

        val comidas = MenuRepository.obtenerOpcionesPorSeccion("Comida principal").ifEmpty { listOf("Pollo", "Carne") }
        val postres = MenuRepository.obtenerOpcionesPorSeccion("Postre").ifEmpty { listOf("Helado", "Alfajor") }

        spinnerComida.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, comidas)
        spinnerPostre.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, postres)

        spinnerComida.setSelection(comidas.indexOf(reserva.comida).coerceAtLeast(0))
        spinnerPostre.setSelection(postres.indexOf(reserva.postre).coerceAtLeast(0))

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFormateada = formatter.format(Date(reserva.fechaMillis))
        tvFecha.text = getString(R.string.fecha_seleccionada, fechaFormateada)

        btnConfirmar.setOnClickListener {
            val actualizada = ReservasRepository.actualizarReserva(
                id = reserva.id,
                comida = spinnerComida.selectedItem.toString(),
                postre = spinnerPostre.selectedItem.toString()
            ) ?: return@setOnClickListener

            val intent = Intent(this, ConfirmacionEdicionActivity::class.java).apply {
                putExtra(ConfirmacionEdicionActivity.EXTRA_FECHA, fechaFormateada)
                putExtra(ConfirmacionEdicionActivity.EXTRA_COMIDA, actualizada.comida)
                putExtra(ConfirmacionEdicionActivity.EXTRA_POSTRE, actualizada.postre)
            }
            startActivity(intent)
        }
    }
}
