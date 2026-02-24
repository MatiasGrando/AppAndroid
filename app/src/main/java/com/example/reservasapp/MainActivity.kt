package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnReservar).setOnClickListener {
            startActivity(Intent(this, ReservarActivity::class.java))
        }

        findViewById<Button>(R.id.btnMisReservas).setOnClickListener {
            startActivity(Intent(this, MisReservasActivity::class.java))
        }

        findViewById<Button>(R.id.btnEditarReservas).setOnClickListener {
            startActivity(Intent(this, EditarReservasActivity::class.java))
        }

        findViewById<Button>(R.id.btnModoAdministrador).setOnClickListener {
            startActivity(Intent(this, AdminMenuActivity::class.java))
        }
    }
}
