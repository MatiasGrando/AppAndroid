package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditarReservasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_reservas)

        val listView = findViewById<ListView>(R.id.listReservasEditar)
        val emptyText = findViewById<TextView>(R.id.tvSinReservasEditar)
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenuEditar)

        val reservas = ReservasRepository.obtenerReservasProximosSieteDias()
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val items = reservas.map {
            val fecha = formatter.format(Date(it.fechaMillis))
            getString(R.string.item_reserva_con_id, it.id, fecha, it.comida, it.postre)
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
                val intent = Intent(this, EditarDetalleReservaActivity::class.java).apply {
                    putExtra(EditarDetalleReservaActivity.EXTRA_RESERVA_ID, reserva.id)
                }
                startActivity(intent)
            }
        }

        btnVolverMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
