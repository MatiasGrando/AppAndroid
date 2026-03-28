package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button

class AdminActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_admin)

        findViewById<Button>(R.id.btnConfigReservasAdmin).setOnClickListener {
            startActivity(Intent(this, AdminConfiguracionReservasActivity::class.java))
        }

        findViewById<Button>(R.id.btnConfigMenuFechaAdmin).setOnClickListener {
            startActivity(Intent(this, AdminMenuFechaActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddMenuAdmin).setOnClickListener {
            startActivity(Intent(this, AdminMenuActivity::class.java))
        }

        findViewById<Button>(R.id.btnResumenPedidosDiaAdmin).setOnClickListener {
            startActivity(Intent(this, PedidosPorDiaActivity::class.java))
        }

        findViewById<Button>(R.id.btnDetallePedidosUsuariosAdmin).setOnClickListener {
            startActivity(Intent(this, DetallePedidosUsuariosActivity::class.java))
        }

        findViewById<Button>(R.id.btnArchivoExportacionAdmin).setOnClickListener {
            startActivity(Intent(this, AdminArchivoExportacionActivity::class.java))
        }
    }
}
