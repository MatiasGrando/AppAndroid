package com.example.reservasapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

object ReservasRepository {
    private const val COLLECTION_RESERVAS = "reservas"
    private const val FIELD_USER_ID = "userId"
    private const val FIELD_FECHA_MILLIS = "fechaMillis"
    private const val FIELD_SELECCIONES = "selecciones"
    private const val FIELD_NOMBRE = "nombre"
    private const val FIELD_APELLIDO = "apellido"
    private const val FIELD_EMPRESA = "empresa"

    private val reservas = mutableListOf<Reserva>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    data class ResultadoAgregarReserva(
        val reservaCreada: Reserva? = null,
        val reservaExistente: Reserva? = null
    )

    fun cargarReservasUsuario(onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            reservas.clear()
            onComplete(false)
            return
        }

        firestore.collection(COLLECTION_RESERVAS)
            .whereEqualTo(FIELD_USER_ID, uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val nuevasReservas = snapshot.documents.mapNotNull { doc ->
                    val fechaMillis = doc.getLong(FIELD_FECHA_MILLIS) ?: return@mapNotNull null
                    val selecciones = (doc.get(FIELD_SELECCIONES) as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            val key = k as? String ?: return@mapNotNull null
                            val value = v as? String ?: return@mapNotNull null
                            key to value
                        }
                        ?.toMap()
                        ?: emptyMap()

                    Reserva(
                        id = doc.id,
                        fechaMillis = fechaMillis,
                        selecciones = selecciones,
                        userId = uid
                    )
                }.sortedBy { it.fechaMillis }

                reservas.clear()
                reservas.addAll(nuevasReservas)
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun agregarReserva(
        fechaMillis: Long,
        selecciones: Map<String, String>,
        onComplete: (ResultadoAgregarReserva) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            onComplete(ResultadoAgregarReserva())
            return
        }

        firestore.collection(COLLECTION_RESERVAS)
            .whereEqualTo(FIELD_USER_ID, uid)
            .whereEqualTo(FIELD_FECHA_MILLIS, fechaMillis)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val existenteDoc = snapshot.documents.firstOrNull()
                if (existenteDoc != null) {
                    val reservaExistente = mapearReserva(existenteDoc, uid)
                    if (reservaExistente != null && reservas.none { it.id == reservaExistente.id }) {
                        reservas.add(reservaExistente)
                    }
                    onComplete(ResultadoAgregarReserva(reservaExistente = reservaExistente))
                    return@addOnSuccessListener
                }

                PerfilRepository.cargarPerfil { perfil ->
                    val payload = mutableMapOf<String, Any>(
                        FIELD_USER_ID to uid,
                        FIELD_FECHA_MILLIS to fechaMillis,
                        FIELD_SELECCIONES to selecciones.toMap(),
                        FIELD_NOMBRE to perfil?.nombre.orEmpty(),
                        FIELD_APELLIDO to perfil?.apellido.orEmpty(),
                        FIELD_EMPRESA to perfil?.empresa.orEmpty()
                    )

                    firestore.collection(COLLECTION_RESERVAS)
                        .add(payload)
                        .addOnSuccessListener { docRef ->
                            val reserva = Reserva(
                                id = docRef.id,
                                fechaMillis = fechaMillis,
                                selecciones = selecciones.toMap(),
                                userId = uid
                            )
                            reservas.add(reserva)
                            onComplete(ResultadoAgregarReserva(reservaCreada = reserva))
                        }
                        .addOnFailureListener {
                            onComplete(ResultadoAgregarReserva())
                        }
                }
            }
            .addOnFailureListener {
                onComplete(ResultadoAgregarReserva())
            }
    }

    private fun mapearReserva(doc: com.google.firebase.firestore.DocumentSnapshot, uid: String): Reserva? {
        val fechaMillis = doc.getLong(FIELD_FECHA_MILLIS) ?: return null
        val selecciones = (doc.get(FIELD_SELECCIONES) as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = v as? String ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            ?: emptyMap()

        return Reserva(
            id = doc.id,
            fechaMillis = fechaMillis,
            selecciones = selecciones,
            userId = uid
        )
    }

    fun actualizarReserva(
        id: String,
        selecciones: Map<String, String>,
        onComplete: (Reserva?) -> Unit
    ) {
        val index = reservas.indexOfFirst { it.id == id }
        if (index == -1) {
            onComplete(null)
            return
        }

        firestore.collection(COLLECTION_RESERVAS)
            .document(id)
            .update(FIELD_SELECCIONES, selecciones.toMap())
            .addOnSuccessListener {
                val actualizada = reservas[index].copy(selecciones = selecciones.toMap())
                reservas[index] = actualizada
                onComplete(actualizada)
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }

    fun obtenerReservaPorId(id: String): Reserva? = reservas.firstOrNull { it.id == id }

    fun obtenerReservaPorFecha(fechaMillis: Long): Reserva? {
        val fechaNormalizada = Calendar.getInstance().apply {
            timeInMillis = fechaMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return reservas.firstOrNull { reserva ->
            val reservaNormalizada = Calendar.getInstance().apply {
                timeInMillis = reserva.fechaMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            reservaNormalizada == fechaNormalizada
        }
    }

    fun obtenerFechasReservadas(): Set<Long> = reservas
        .map { reserva ->
            Calendar.getInstance().apply {
                timeInMillis = reserva.fechaMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        .toSet()

    fun obtenerReservasProximosSieteDias(): List<Reserva> {
        val inicioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finRango = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.DAY_OF_YEAR, 6)
        }.timeInMillis

        return reservas
            .filter { it.fechaMillis in inicioHoy..finRango }
            .sortedBy { it.fechaMillis }
    }

    fun formatearSelecciones(selecciones: Map<String, String>): String {
        return selecciones.entries.joinToString(" | ") { "${it.key}: ${it.value}" }
    }
}
