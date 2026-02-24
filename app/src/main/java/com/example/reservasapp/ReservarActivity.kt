package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservarActivity : AppCompatActivity() {
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservar)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val continueButton = findViewById<Button>(R.id.btnContinuar)

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        selectedDate = formatter.format(calendarView.date)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            selectedDate = formatter.format(calendar.time)
        }

        continueButton.setOnClickListener {
            val intent = Intent(this, DetalleReservaActivity::class.java)
            intent.putExtra(DetalleReservaActivity.EXTRA_DATE, selectedDate)
            startActivity(intent)
        }
    }
}
