package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity

class ReservarActivity : AppCompatActivity() {
    private var selectedDateMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservar)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        val continueButton = findViewById<Button>(R.id.btnContinuar)

        selectedDateMillis = calendarView.date

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDateMillis = java.util.Calendar.getInstance().apply {
                set(year, month, dayOfMonth, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        continueButton.setOnClickListener {
            val intent = Intent(this, DetalleReservaActivity::class.java)
            intent.putExtra(DetalleReservaActivity.EXTRA_DATE_MILLIS, selectedDateMillis)
            startActivity(intent)
        }
    }
}
