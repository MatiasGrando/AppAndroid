package com.example.reservasapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

class PedidosActivity : AppCompatActivity() {

    private val firestoreRepository = FirestoreRepository()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos)

        val recyclerView = findViewById<RecyclerView>(R.id.rvPedidos)
        val emptyText = findViewById<TextView>(R.id.tvSinPedidos)

        val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val adapter = PedidosAdapter(emptyList(), AppSession.isAdmin) { pedido, nuevoEstado ->
            firestoreRepository.actualizarEstadoPedido(pedido.idPedido, nuevoEstado) {
                if (it.isFailure) Toast.makeText(this, R.string.error_actualizar_estado, Toast.LENGTH_SHORT).show()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        listener = firestoreRepository.observarPedidos(userId, AppSession.isAdmin) { result ->
            val pedidos = result.getOrElse {
                Toast.makeText(this, R.string.error_cargar_pedidos, Toast.LENGTH_SHORT).show()
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                return@observarPedidos
            }

            adapter.actualizarPedidos(pedidos)
            emptyText.visibility = if (pedidos.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (pedidos.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroy() {
        listener?.remove()
        super.onDestroy()
    }
}
