package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        findViewById<Button>(R.id.btnAddMenuAdmin).setOnClickListener {
            startActivity(Intent(this, AdminMenuActivity::class.java))
        }
    }
}
