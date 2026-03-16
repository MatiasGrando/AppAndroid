package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarReservasActivity : BaseActivity() {

    private lateinit var listView: ListView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_reservas)

        listView = findViewById(R.id.listReservasEditar)
        emptyText = findViewById(R.id.tvSinReservasEditar)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuEditar)

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
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val items = reservas.map {
            val fecha = formatter.format(Date(it.fechaMillis))
            val detalle = ReservasRepository.formatearSelecciones(it.selecciones)
            getString(R.string.item_reserva_con_id_generico, it.id, fecha, detalle)
        }

        if (items.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)

            listView.setOnItemClickListener { _, _, position, _ ->
                val reserva = reservas[position]
                val intent = Intent(this, DetalleReservaActivity::class.java).apply {
                    putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reserva.id)
                    putExtra(DetalleReservaActivity.EXTRA_DATE_MILLIS, reserva.fechaMillis)
                }
                startActivity(intent)
            }
        }
    }
}
