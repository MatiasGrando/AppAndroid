package com.example.reservasapp

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MisReservasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_reservas)

        val listView = findViewById<ListView>(R.id.listReservas)
        val emptyText = findViewById<TextView>(R.id.tvSinReservas)

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val items = ReservasRepository.obtenerReservasProximosSieteDias().map {
            val fecha = formatter.format(Date(it.fechaMillis))
            getString(R.string.item_reserva, fecha, it.comida, it.postre)
        }

        if (items.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            listView.visibility = View.VISIBLE
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        }
    }
}
