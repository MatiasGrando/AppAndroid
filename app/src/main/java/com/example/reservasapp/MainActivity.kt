package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val authRepository by lazy { AuthRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnReservar).setOnClickListener {
            startActivity(Intent(this, ReservarActivity::class.java))
        }

        findViewById<Button>(R.id.btnMisReservas).setOnClickListener {
            startActivity(Intent(this, PedidosActivity::class.java))
        }

        findViewById<Button>(R.id.btnAdminPedidos).apply {
            visibility = if (AppSession.isAdmin) View.VISIBLE else View.GONE
            setOnClickListener {
                startActivity(Intent(this@MainActivity, PedidosActivity::class.java))
            }
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            authRepository.logout(this) {
                AppSession.currentUsuario = null
                AppSession.isAdmin = false
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}
