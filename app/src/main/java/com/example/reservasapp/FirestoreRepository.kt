package com.example.reservasapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun obtenerUsuario(userId: String, onResult: (Result<Usuario?>) -> Unit) {
        db.collection(COL_USUARIOS).document(userId).get()
            .addOnSuccessListener { doc ->
                onResult(Result.success(doc.toObject(Usuario::class.java)))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun guardarUsuario(usuario: Usuario, onResult: (Result<Unit>) -> Unit) {
        db.collection(COL_USUARIOS)
            .document(usuario.userId)
            .set(usuario)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun esAdmin(userId: String, onResult: (Result<Boolean>) -> Unit) {
        db.collection(COL_ADMINS).document(userId).get()
            .addOnSuccessListener { doc -> onResult(Result.success(doc.exists())) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun guardarPedido(pedido: Pedido, onResult: (Result<String>) -> Unit) {
        val pedidoRef = if (pedido.idPedido.isBlank()) {
            db.collection(COL_PEDIDOS).document()
        } else {
            db.collection(COL_PEDIDOS).document(pedido.idPedido)
        }

        val payload = pedido.copy(idPedido = pedidoRef.id)
        pedidoRef.set(payload)
            .addOnSuccessListener { onResult(Result.success(pedidoRef.id)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    fun observarPedidos(userId: String, esAdmin: Boolean, onResult: (Result<List<Pedido>>) -> Unit): ListenerRegistration {
        val query = if (esAdmin) {
            db.collection(COL_PEDIDOS)
        } else {
            db.collection(COL_PEDIDOS).whereEqualTo("userId", userId)
        }

        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(Result.failure(error))
                return@addSnapshotListener
            }
            val pedidos = snapshot?.documents.orEmpty().mapNotNull { it.toObject(Pedido::class.java) }
                .sortedByDescending { it.fecha }
            onResult(Result.success(pedidos))
        }
    }

    fun actualizarEstadoPedido(idPedido: String, nuevoEstado: String, onResult: (Result<Unit>) -> Unit) {
        db.collection(COL_PEDIDOS).document(idPedido)
            .update("estadoPedido", nuevoEstado)
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    companion object {
        const val COL_USUARIOS = "usuarios"
        const val COL_PEDIDOS = "pedidos"
        const val COL_ADMINS = "admins"
    }
}
