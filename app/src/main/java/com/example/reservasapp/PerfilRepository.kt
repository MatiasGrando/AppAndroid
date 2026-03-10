package com.example.reservasapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class PerfilUsuario(
    val nombre: String = "",
    val apellido: String = "",
    val empresa: String = "",
    val dni: String = ""
)

fun PerfilUsuario.estaCompleto(): Boolean {
    return nombre.isNotBlank() &&
        apellido.isNotBlank() &&
        empresa.isNotBlank() &&
        dni.isNotBlank()
}

object PerfilRepository {
    private const val COLLECTION_PERFILES = "perfiles"
    private const val FIELD_NOMBRE = "nombre"
    private const val FIELD_APELLIDO = "apellido"
    private const val FIELD_EMPRESA = "empresa"
    private const val FIELD_DNI = "dni"
    private const val FIELD_ADMIN = "admin"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    fun cargarPerfil(onComplete: (PerfilUsuario?) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onComplete(null)
            return
        }

        firestore.collection(COLLECTION_PERFILES)
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val perfil = PerfilUsuario(
                    nombre = snapshot.getString(FIELD_NOMBRE).orEmpty(),
                    apellido = snapshot.getString(FIELD_APELLIDO).orEmpty(),
                    empresa = snapshot.getString(FIELD_EMPRESA).orEmpty(),
                    dni = snapshot.getString(FIELD_DNI).orEmpty()
                )
                onComplete(perfil)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun guardarPerfil(perfil: PerfilUsuario, onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onComplete(false)
            return
        }

        val payload = mutableMapOf<String, Any>(
            FIELD_NOMBRE to perfil.nombre.trim(),
            FIELD_APELLIDO to perfil.apellido.trim(),
            FIELD_EMPRESA to perfil.empresa.trim(),
            FIELD_DNI to perfil.dni.trim()
        )

        val docRef = firestore.collection(COLLECTION_PERFILES).document(uid)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.contains(FIELD_ADMIN)) {
                    payload[FIELD_ADMIN] = false
                }

                docRef.set(payload, SetOptions.merge())
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun sincronizarPerfilConGoogle(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            onComplete(false)
            return
        }

        val displayName = user.displayName.orEmpty().trim()
        val nombreGoogle = displayName.substringBefore(' ', "").trim()
        val apellidoGoogle = displayName.substringAfter(' ', "").trim().takeIf { it != displayName } ?: ""

        firestore.collection(COLLECTION_PERFILES)
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val perfilActual = PerfilUsuario(
                    nombre = snapshot.getString(FIELD_NOMBRE).orEmpty(),
                    apellido = snapshot.getString(FIELD_APELLIDO).orEmpty(),
                    empresa = snapshot.getString(FIELD_EMPRESA).orEmpty(),
                    dni = snapshot.getString(FIELD_DNI).orEmpty()
                )

                val perfilSincronizado = perfilActual.copy(
                    nombre = perfilActual.nombre.ifBlank { nombreGoogle },
                    apellido = perfilActual.apellido.ifBlank { apellidoGoogle }
                )

                guardarPerfil(perfilSincronizado, onComplete)
            }
            .addOnFailureListener {
                val fallback = PerfilUsuario(nombre = nombreGoogle, apellido = apellidoGoogle)
                guardarPerfil(fallback, onComplete)
            }
    }

    fun obtenerEsAdmin(onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onComplete(false)
            return
        }

        firestore.collection(COLLECTION_PERFILES)
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                onComplete(snapshot.getBoolean(FIELD_ADMIN) ?: false)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}
