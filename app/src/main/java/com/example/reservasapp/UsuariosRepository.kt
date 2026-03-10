package com.example.reservasapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

object UsuariosRepository {
    private const val COLLECTION_USUARIOS = "usuarios"
    private const val FIELD_NOMBRE = "nombre"
    private const val FIELD_EMAIL = "email"
    private const val FIELD_ADMIN = "admin"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun sincronizarUsuarioYObtenerRol(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        val uid = user?.uid

        if (uid.isNullOrBlank()) {
            onComplete(false)
            return
        }

        val docRef = firestore.collection(COLLECTION_USUARIOS).document(uid)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                val esAdmin = snapshot.getBoolean(FIELD_ADMIN) ?: false

                val updates = mutableMapOf<String, Any>()
                if (!snapshot.contains(FIELD_ADMIN)) {
                    updates[FIELD_ADMIN] = false
                }
                if (!snapshot.contains(FIELD_NOMBRE) && !user.displayName.isNullOrBlank()) {
                    updates[FIELD_NOMBRE] = user.displayName.orEmpty().trim()
                }
                if (!snapshot.contains(FIELD_EMAIL) && !user.email.isNullOrBlank()) {
                    updates[FIELD_EMAIL] = user.email.orEmpty().trim()
                }

                if (updates.isEmpty()) {
                    onComplete(esAdmin)
                    return@addOnSuccessListener
                }

                docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener { onComplete(esAdmin) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}
