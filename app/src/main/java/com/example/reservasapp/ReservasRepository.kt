package com.example.reservasapp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

object ReservasRepository {
    private const val COLLECTION_RESERVAS = "reservas"
    private const val FIELD_USER_ID = "userId"
    private const val FIELD_FECHA_MILLIS = "fechaMillis"
    private const val FIELD_SELECCIONES = "selecciones"
    private const val FIELD_NOMBRE = "nombre"
    private const val FIELD_APELLIDO = "apellido"
    private const val FIELD_EMPRESA = "empresa"
    private const val RESERVA_WINDOW_DAYS = 6
    private const val SECTION_PLATO_PRINCIPAL = "Plato principal"
    private const val SECTION_GUARNICIONES = "Guarniciones"

    private val reservas = mutableListOf<Reserva>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    data class DetallePedidoUsuario(
        val empresa: String,
        val nombre: String,
        val apellido: String,
        val platoPrincipal: String,
        val guarnicion: String,
        val postre: String
    )

    data class ResultadoAgregarReserva(
        val reservaCreada: Reserva? = null,
        val reservaExistente: Reserva? = null
    )

    fun clearCache() {
        reservas.clear()
    }

    fun esFechaReservable(fechaMillis: Long): Boolean {
        val fechaNormalizada = normalizarFecha(fechaMillis)
        return fechaNormalizada in ventanaReservaActual()
    }

    fun cargarReservasUsuario(onComplete: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            clearCache()
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
                        fechaMillis = normalizarFecha(fechaMillis),
                        selecciones = selecciones,
                        userId = uid
                    )
                }.sortedBy { it.fechaMillis }

                clearCache()
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
        val fechaNormalizada = normalizarFecha(fechaMillis)
        val seleccionesNormalizadas = sanitizeSelecciones(selecciones)
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank() || seleccionesNormalizadas.isEmpty() || !esFechaReservable(fechaNormalizada)) {
            onComplete(ResultadoAgregarReserva())
            return
        }

        validarSeleccionesParaFecha(fechaNormalizada, seleccionesNormalizadas) { seleccionesValidas ->
            if (!seleccionesValidas) {
                onComplete(ResultadoAgregarReserva())
                return@validarSeleccionesParaFecha
            }

            firestore.collection(COLLECTION_RESERVAS)
                .whereEqualTo(FIELD_USER_ID, uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    val existenteDoc = snapshot.documents.firstOrNull { doc ->
                        val fechaExistente = doc.getLong(FIELD_FECHA_MILLIS) ?: return@firstOrNull false
                        normalizarFecha(fechaExistente) == fechaNormalizada
                    }
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
                            FIELD_FECHA_MILLIS to fechaNormalizada,
                            FIELD_SELECCIONES to seleccionesNormalizadas,
                            FIELD_NOMBRE to perfil?.nombre.orEmpty(),
                            FIELD_APELLIDO to perfil?.apellido.orEmpty(),
                            FIELD_EMPRESA to perfil?.empresa.orEmpty()
                        )

                        firestore.collection(COLLECTION_RESERVAS)
                            .add(payload)
                            .addOnSuccessListener { docRef ->
                                val reserva = Reserva(
                                    id = docRef.id,
                                    fechaMillis = fechaNormalizada,
                                    selecciones = seleccionesNormalizadas,
                                    userId = uid
                                )
                                reservas.add(reserva)
                                reservas.sortBy { it.fechaMillis }
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
            fechaMillis = normalizarFecha(fechaMillis),
            selecciones = selecciones,
            userId = uid
        )
    }

    fun actualizarReserva(
        id: String,
        selecciones: Map<String, String>,
        onComplete: (Reserva?) -> Unit
    ) {
        val seleccionesNormalizadas = sanitizeSelecciones(selecciones)
        val index = reservas.indexOfFirst { it.id == id }
        val reservaActual = reservas.getOrNull(index)
        if (reservaActual == null || seleccionesNormalizadas.isEmpty() || !esFechaReservable(reservaActual.fechaMillis)) {
            onComplete(null)
            return
        }

        validarSeleccionesParaFecha(reservaActual.fechaMillis, seleccionesNormalizadas) { seleccionesValidas ->
            if (!seleccionesValidas) {
                onComplete(null)
                return@validarSeleccionesParaFecha
            }

            firestore.collection(COLLECTION_RESERVAS)
                .document(id)
                .update(FIELD_SELECCIONES, seleccionesNormalizadas)
                .addOnSuccessListener {
                    val actualizada = reservaActual.copy(selecciones = seleccionesNormalizadas)
                    reservas[index] = actualizada
                    onComplete(actualizada)
                }
                .addOnFailureListener {
                    onComplete(null)
                }
        }
    }

    fun obtenerReservaPorId(id: String): Reserva? = reservas.firstOrNull { it.id == id }

    fun obtenerReservaPorFecha(fechaMillis: Long): Reserva? {
        val fechaNormalizada = normalizarFecha(fechaMillis)
        return reservas.firstOrNull { it.fechaMillis == fechaNormalizada }
    }

    fun obtenerFechasReservadas(): Set<Long> = reservas
        .map { reserva -> reserva.fechaMillis }
        .toSet()

    fun obtenerReservasProximosSieteDias(): List<Reserva> {
        val ventanaReservaActual = ventanaReservaActual()

        return reservas
            .filter { it.fechaMillis in ventanaReservaActual }
            .sortedBy { it.fechaMillis }
    }



    fun obtenerResumenPedidosPorFecha(
        fechaMillis: Long,
        onComplete: (Boolean, Map<String, Map<String, Int>>) -> Unit
    ) {
        val inicioDia = Calendar.getInstance().apply {
            timeInMillis = fechaMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finDia = Calendar.getInstance().apply {
            timeInMillis = inicioDia
            add(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis

        firestore.collection(COLLECTION_RESERVAS)
            .whereGreaterThanOrEqualTo(FIELD_FECHA_MILLIS, inicioDia)
            .whereLessThanOrEqualTo(FIELD_FECHA_MILLIS, finDia)
            .get()
            .addOnSuccessListener { snapshot ->
                val groupedCounts = linkedMapOf<String, MutableMap<String, Int>>()

                snapshot.documents.forEach { doc ->
                    val selecciones = (doc.get(FIELD_SELECCIONES) as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            val key = k as? String ?: return@mapNotNull null
                            val value = v as? String ?: return@mapNotNull null
                            key to value
                        }
                        ?.toMap()
                        ?: emptyMap()

                    selecciones.forEach { (tipoComida, plato) ->
                        val dishCounters = groupedCounts.getOrPut(tipoComida) { linkedMapOf() }
                        dishCounters[plato] = (dishCounters[plato] ?: 0) + 1
                    }
                }

                onComplete(true, groupedCounts)
            }
            .addOnFailureListener {
                onComplete(false, emptyMap())
            }
    }

    fun obtenerDetallePedidosPorFecha(
        fechaMillis: Long,
        onComplete: (Boolean, List<DetallePedidoUsuario>) -> Unit
    ) {
        val inicioDia = Calendar.getInstance().apply {
            timeInMillis = fechaMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finDia = Calendar.getInstance().apply {
            timeInMillis = inicioDia
            add(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis

        firestore.collection(COLLECTION_RESERVAS)
            .whereGreaterThanOrEqualTo(FIELD_FECHA_MILLIS, inicioDia)
            .whereLessThanOrEqualTo(FIELD_FECHA_MILLIS, finDia)
            .get()
            .addOnSuccessListener { snapshot ->
                val rows = snapshot.documents.map { doc ->
                    val selecciones = (doc.get(FIELD_SELECCIONES) as? Map<*, *>)
                        ?.mapNotNull { (k, v) ->
                            val key = k as? String ?: return@mapNotNull null
                            val value = v as? String ?: return@mapNotNull null
                            key to value
                        }
                        ?.toMap()
                        ?: emptyMap()

                    DetallePedidoUsuario(
                        empresa = doc.getString(FIELD_EMPRESA).orEmpty().ifBlank { "Sin empresa" },
                        nombre = doc.getString(FIELD_NOMBRE).orEmpty(),
                        apellido = doc.getString(FIELD_APELLIDO).orEmpty(),
                        platoPrincipal = selecciones["Plato principal"].orDash(),
                        guarnicion = selecciones["Guarniciones"].orDash(),
                        postre = selecciones["Postres"].orDash()
                    )
                }.sortedWith(
                    compareBy<DetallePedidoUsuario> { it.empresa.lowercase() }
                        .thenBy { it.apellido.lowercase() }
                        .thenBy { it.nombre.lowercase() }
                )

                onComplete(true, rows)
            }
            .addOnFailureListener {
                onComplete(false, emptyList())
            }
    }

    fun formatearSelecciones(selecciones: Map<String, String>): String {
        return sanitizeSelecciones(selecciones)
            .entries
            .joinToString(" | ") { "${it.key}: ${it.value}" }
    }

    internal fun sanitizeSelecciones(selecciones: Map<String, String>): Map<String, String> {
        val normalizadas = linkedMapOf<String, String>()

        selecciones.forEach { (tipoComida, plato) ->
            val tipoNormalizado = tipoComida.trim()
            val platoNormalizado = plato.trim()
            if (tipoNormalizado.isBlank() || platoNormalizado.isBlank()) {
                return@forEach
            }

            normalizadas[tipoNormalizado] = platoNormalizado
        }

        return normalizadas
    }

    private fun validarSeleccionesParaFecha(
        fechaMillis: Long,
        selecciones: Map<String, String>,
        onComplete: (Boolean) -> Unit
    ) {
        MenuRepository.cargarSecciones(fechaMillis) { ok, secciones ->
            if (!ok) {
                onComplete(false)
                return@cargarSecciones
            }

            onComplete(seleccionesSonValidasParaMenu(secciones, selecciones))
        }
    }

    internal fun seleccionesSonValidasParaMenu(
        secciones: List<MenuSection>,
        selecciones: Map<String, String>
    ): Boolean {
        if (selecciones.isEmpty()) return false

        val seccionesPorClave = secciones.associateBy { it.nombre.normalizarClaveDominio() }
        val seleccionesPorClave = selecciones.mapKeys { it.key.normalizarClaveDominio() }

        val platoPrincipal = seccionesPorClave[SECTION_PLATO_PRINCIPAL.normalizarClaveDominio()]
            ?.opciones
            ?.firstOrNull { plato ->
                plato.nombre.normalizarClaveDominio() == seleccionesPorClave[SECTION_PLATO_PRINCIPAL.normalizarClaveDominio()]?.normalizarClaveDominio()
            }
            ?: return false

        val guarnicionSeleccionada = seleccionesPorClave[SECTION_GUARNICIONES.normalizarClaveDominio()]
        if (!platoPrincipal.guarnicion && !guarnicionSeleccionada.isNullOrBlank()) {
            return false
        }

        return selecciones.all { (tipoComida, plato) ->
            seccionesPorClave[tipoComida.normalizarClaveDominio()]
                ?.opciones
                ?.any { opcion -> opcion.nombre.normalizarClaveDominio() == plato.normalizarClaveDominio() } == true
        }
    }

    private fun normalizarFecha(fechaMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = fechaMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun ventanaReservaActual(): LongRange {
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
            add(Calendar.DAY_OF_YEAR, RESERVA_WINDOW_DAYS)
        }.timeInMillis

        return inicioHoy..finRango
    }

    private fun String.normalizarClaveDominio(): String {
        return trim().lowercase(Locale.ROOT)
    }

    private fun String?.orDash(): String = if (this.isNullOrBlank()) "-" else this
}
