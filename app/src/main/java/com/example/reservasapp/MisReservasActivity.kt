package com.example.reservasapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reservasapp.branding.AppRuntime

class MisReservasActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var emptyText: TextView
    private lateinit var editButton: Button
    private lateinit var backButton: Button
    private var selectedReserva: Reserva? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_reservas)

        recyclerView = findViewById(R.id.rvReservas)
        titleText = findViewById(R.id.tvTituloMisReservas)
        emptyText = findViewById(R.id.tvSinReservas)
        editButton = findViewById(R.id.btnEditarMisReservas)
        backButton = findViewById(R.id.btnVolverMenuMisReservas)

        applyBranding()

        recyclerView.layoutManager = LinearLayoutManager(this)
        updateEditButtonState()

        editButton.setOnClickListener {
            val reserva = selectedReserva ?: return@setOnClickListener
            startActivity(DetalleReservaActivity.editIntent(this, reserva))
        }

        backButton.setOnClickListener {
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
        val branding = AppRuntime.branding
        val canEditSelection = selectedReserva?.let { reserva ->
            ReservasRepository.puedeEditarReservaExistenteEnFecha(reserva.fechaMillis)
        } == true

        val baseColor = ContextCompat.getColor(
            this,
            if (canEditSelection) branding.primaryActionColorRes else branding.secondaryActionColorRes
        )

        editButton.isEnabled = canEditSelection
        editButton.alpha = if (canEditSelection) 1f else 0.85f
        editButton.backgroundTintList = ColorStateList.valueOf(
            if (canEditSelection) baseColor else ColorUtils.setAlphaComponent(baseColor, (255 * 0.72f).toInt())
        )
        editButton.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
    }

    private fun applyBranding() {
        val branding = AppRuntime.branding
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val titleColor = ContextCompat.getColor(this, branding.confirmationTitleColorRes)
        val bodyColor = ContextCompat.getColor(this, branding.confirmationBodyTextColorRes)

        root.setBackgroundResource(branding.homeBackgroundRes)
        titleText.setTextColor(titleColor)
        emptyText.setTextColor(bodyColor)
        backButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, branding.secondaryActionColorRes)
        )
        backButton.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
        title = getString(branding.appNameRes)
    }

}
