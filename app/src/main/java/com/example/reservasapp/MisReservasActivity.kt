package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MisReservasActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_reservas)

        recyclerView = findViewById(R.id.rvReservas)
        emptyText = findViewById(R.id.tvSinReservas)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuMisReservas)

        recyclerView.layoutManager = LinearLayoutManager(this)

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
        ReservasRepository.cargarReservasUsuario { ok ->
            if (!ok) {
                Toast.makeText(this, R.string.error_cargar_reservas, Toast.LENGTH_SHORT).show()
            }
            renderReservas()
        }
    }

    private fun renderReservas() {
        val reservas = ReservasRepository.obtenerReservasProximosSieteDias()
        recyclerView.adapter = MisReservasAdapter(reservas)

        if (reservas.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
}
