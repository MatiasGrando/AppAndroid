package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MisReservasActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var editButton: Button
    private var selectedReserva: Reserva? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_reservas)

        recyclerView = findViewById(R.id.rvReservas)
        emptyText = findViewById(R.id.tvSinReservas)
        editButton = findViewById(R.id.btnEditarMisReservas)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuMisReservas)

        recyclerView.layoutManager = LinearLayoutManager(this)
        updateEditButtonState()

        editButton.setOnClickListener {
            val reserva = selectedReserva ?: return@setOnClickListener
            startActivity(DetalleReservaActivity.editIntent(this, reserva))
        }

        btnVolverMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        MenuRepository.cargarSecciones { _, _ ->
            ReservasRepository.cargarReservasUsuario { ok ->
                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_reservas, Toast.LENGTH_SHORT).show()
                }
                renderReservas()
            }
        }
    }

    private fun renderReservas() {
        val reservas = ReservasRepository.obtenerReservasProximosSieteDias()
        val imageUrlsByDish = MenuRepository.obtenerSecciones()
            .flatMap { seccion -> seccion.opciones }
            .associate { plato -> plato.id to plato.imageUrl }

        recyclerView.adapter = MisReservasAdapter(
            reservas = reservas,
            imageUrlsByDish = imageUrlsByDish
        ) { reservaSeleccionada ->
            selectedReserva = reservaSeleccionada
            updateEditButtonState()
        }

        selectedReserva = null
        updateEditButtonState()

        if (reservas.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateEditButtonState() {
        val canEditSelection = selectedReserva?.let { reserva ->
            ReservasRepository.puedeEditarReservaExistenteEnFecha(reserva.fechaMillis)
        } == true
        editButton.isEnabled = canEditSelection
        editButton.alpha = if (canEditSelection) 1f else 0.85f
        editButton.setBackgroundResource(
            if (canEditSelection) R.drawable.bg_button_orange else R.drawable.bg_button_gray
        )
    }

}
