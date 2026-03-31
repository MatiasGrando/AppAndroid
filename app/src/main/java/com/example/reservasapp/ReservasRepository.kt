package com.example.reservasapp

import com.example.reservasapp.firebase.FirebaseProvider
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReservasRepository {
    private const val COLLECTION_RESERVAS = "reservas"
    private const val COLLECTION_RESERVAS_ARCHIVO = "reservas_archivo"
    private const val COLLECTION_RESERVAS_ARCHIVO_OPERACIONES = "reservas_archivo_operaciones"
    private const val FIELD_USER_ID = "userId"
    private const val FIELD_FECHA_MILLIS = "fechaMillis"
    private const val FIELD_SELECCIONES = "selecciones"
    private const val FIELD_NOMBRE = "nombre"
    private const val FIELD_APELLIDO = "apellido"
    private const val FIELD_EMPRESA = "empresa"
    private const val FIELD_RESERVA_ID = "reservaId"
    private const val FIELD_ARCHIVADO_EN = "archivadoEn"
    private const val FIELD_ARCHIVO_OPERACION_ID = "archivoOperacionId"
    private const val FIELD_ESTADO = "estado"
    private const val FIELD_OPERADOR_UID = "operadorUid"
    private const val FIELD_OPERADOR_EMAIL = "operadorEmail"
    private const val FIELD_ARCHIVO_NOMBRE = "archivoNombre"
    private const val FIELD_RESERVA_IDS = "reservaIds"
    private const val FIELD_ERROR_MESSAGE = "errorMessage"
    private const val FIELD_INICIADO_EN = "iniciadoEn"
    private const val FIELD_FINALIZADO_EN = "finalizadoEn"
    private const val FIELD_DESDE_MILLIS = "desdeMillis"
    private const val FIELD_HASTA_MILLIS = "hastaMillis"
    private const val FIELD_CANTIDAD_RESERVAS = "cantidadReservas"
    private const val MAX_ARCHIVE_OPERATION_ROWS = 249
    private const val ESTADO_INICIADO = "iniciado"
    private const val ESTADO_CSV_GUARDADO = "csv_guardado"
    private const val ESTADO_ARCHIVADO = "archivado"
    private const val ESTADO_CANCELADO = "cancelado"
    private const val ESTADO_ERROR = "error"
    private const val SECTION_PLATO_PRINCIPAL = MenuIdentity.SECTION_MAIN
    private const val SECTION_GUARNICIONES = MenuIdentity.SECTION_SIDE
    private const val SECTION_POSTRES = MenuIdentity.SECTION_DESSERT

    private val reservas = mutableListOf<Reserva>()
    private val firestore by lazy { FirebaseProvider.firestore() }
    private val auth by lazy { FirebaseProvider.auth() }

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

    private data class ResultadoAgregarReservaTransaccional(
        val reservaCreada: Reserva? = null,
        val reservaExistente: Reserva? = null
    )

    data class ReservaExportable(
        val id: String,
        val fechaMillis: Long,
        val empresa: String,
        val apellido: String,
        val nombre: String,
        val userId: String,
        val platoPrincipal: String,
        val guarnicion: String,
        val postre: String,
        val seleccionesMap: Map<String, String>,
        val selecciones: String
    )

    data class ArchivoReservasResult(
        val operacionId: String,
        val ultimaFechaArchivadaMillis: Long,
        val cantidadReservas: Int
    )

    data class ArchivoReservasOperacion(
        val id: String,
        val archivoNombre: String,
        val operadorUid: String,
        val operadorEmail: String?
    )

    fun clearCache() {
        reservas.clear()
    }

    fun esFechaReservable(fechaMillis: Long): Boolean {
        val fechaNormalizada = normalizarFecha(fechaMillis)
        return fechaNormalizada in ventanaEdicionActual()
    }

    fun puedeCrearReservaEnFecha(fechaMillis: Long): Boolean {
        val fechaNormalizada = normalizarFecha(fechaMillis)
        return fechaNormalizada in ventanaCreacionActual() &&
            BookingAvailabilityRepository.estaFechaHabilitada(fechaNormalizada)
    }

    fun puedeEditarReservaExistenteEnFecha(fechaMillis: Long): Boolean {
        val fechaNormalizada = normalizarFecha(fechaMillis)
        return fechaNormalizada in ventanaEdicionPermitidaActual()
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
        if (uid.isNullOrBlank() || seleccionesNormalizadas.isEmpty()) {
            onComplete(ResultadoAgregarReserva())
            return
        }

        BookingAvailabilityRepository.cargarConfiguracion { _, _ ->
            if (!puedeCrearReservaEnFecha(fechaNormalizada)) {
                onComplete(ResultadoAgregarReserva())
                return@cargarConfiguracion
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

                            agregarReservaConDocumentoDeterministico(
                                uid = uid,
                                fechaNormalizada = fechaNormalizada,
                                seleccionesNormalizadas = seleccionesNormalizadas,
                                payload = payload,
                                onComplete = onComplete
                            )
                        }
                    }
                    .addOnFailureListener {
                        onComplete(ResultadoAgregarReserva())
                    }
            }
        }
    }

    private fun agregarReservaConDocumentoDeterministico(
        uid: String,
        fechaNormalizada: Long,
        seleccionesNormalizadas: Map<String, String>,
        payload: Map<String, Any>,
        onComplete: (ResultadoAgregarReserva) -> Unit
    ) {
        val documentId = buildReservaDocumentId(uid, fechaNormalizada)
        val reservaRef = firestore.collection(COLLECTION_RESERVAS).document(documentId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reservaRef)
            if (snapshot.exists()) {
                ResultadoAgregarReservaTransaccional(
                    reservaExistente = mapearReserva(snapshot, uid) ?: Reserva(
                        id = snapshot.id,
                        fechaMillis = fechaNormalizada,
                        selecciones = extraerSelecciones(snapshot),
                        userId = uid
                    )
                )
            } else {
                transaction.set(reservaRef, payload)
                ResultadoAgregarReservaTransaccional(
                    reservaCreada = Reserva(
                        id = documentId,
                        fechaMillis = fechaNormalizada,
                        selecciones = seleccionesNormalizadas,
                        userId = uid
                    )
                )
            }
        }
            .addOnSuccessListener { resultado ->
                when {
                    resultado.reservaExistente != null -> {
                        val reservaExistente = resultado.reservaExistente
                        if (reservas.none { it.id == reservaExistente.id }) {
                            reservas.add(reservaExistente)
                            reservas.sortBy { it.fechaMillis }
                        }
                        onComplete(ResultadoAgregarReserva(reservaExistente = reservaExistente))
                    }

                    resultado.reservaCreada != null -> {
                        val reservaCreada = resultado.reservaCreada
                        reservas.removeAll { it.id == reservaCreada.id }
                        reservas.add(reservaCreada)
                        reservas.sortBy { it.fechaMillis }
                        onComplete(ResultadoAgregarReserva(reservaCreada = reservaCreada))
                    }

                    else -> onComplete(ResultadoAgregarReserva())
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
        if (reservaActual == null || seleccionesNormalizadas.isEmpty() || !puedeEditarReservaExistenteEnFecha(reservaActual.fechaMillis)) {
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

    fun obtenerReservasProximosSieteDias(): List<Reserva> {
        val ventanaReservaActual = ventanaEdicionActual()

        return reservas
            .filter { it.fechaMillis in ventanaReservaActual }
            .sortedBy { it.fechaMillis }
    }



    fun obtenerResumenPedidosPorFecha(
        fechaMillis: Long,
        onComplete: (Boolean, Map<String, Map<String, Int>>) -> Unit
    ) {
        val (inicioDia, finDia) = dayBounds(fechaMillis)

        MenuRepository.cargarSecciones { okMenu, _ ->
            if (!okMenu) {
                onComplete(false, emptyMap())
                return@cargarSecciones
            }

            firestore.collection(COLLECTION_RESERVAS)
                .whereGreaterThanOrEqualTo(FIELD_FECHA_MILLIS, inicioDia)
                .whereLessThanOrEqualTo(FIELD_FECHA_MILLIS, finDia)
                .get()
                .addOnSuccessListener { snapshot ->
                    val groupedCounts = linkedMapOf<String, MutableMap<String, Int>>()

                    snapshot.documents.forEach { doc ->
                        val selecciones = extraerSelecciones(doc)
                        MenuRepository.resolveSelections(selecciones).forEach { (sectionName, dishName) ->
                            val dishCounters = groupedCounts.getOrPut(sectionName) { linkedMapOf() }
                            dishCounters[dishName] = (dishCounters[dishName] ?: 0) + 1
                        }
                    }

                    onComplete(true, groupedCounts)
                }
                .addOnFailureListener {
                    onComplete(false, emptyMap())
                }
        }
    }

    fun obtenerDetallePedidosPorFecha(
        fechaMillis: Long,
        onComplete: (Boolean, List<DetallePedidoUsuario>) -> Unit
    ) {
        val (inicioDia, finDia) = dayBounds(fechaMillis)

        MenuRepository.cargarSecciones { okMenu, _ ->
            if (!okMenu) {
                onComplete(false, emptyList())
                return@cargarSecciones
            }

            firestore.collection(COLLECTION_RESERVAS)
                .whereGreaterThanOrEqualTo(FIELD_FECHA_MILLIS, inicioDia)
                .whereLessThanOrEqualTo(FIELD_FECHA_MILLIS, finDia)
                .get()
                .addOnSuccessListener { snapshot ->
                    val rows = snapshot.documents.map { doc ->
                        val selecciones = extraerSelecciones(doc)

                        DetallePedidoUsuario(
                            empresa = doc.getString(FIELD_EMPRESA).orEmpty().ifBlank { "Sin empresa" },
                            nombre = doc.getString(FIELD_NOMBRE).orEmpty(),
                            apellido = doc.getString(FIELD_APELLIDO).orEmpty(),
                            platoPrincipal = MenuRepository.nombrePlato(SECTION_PLATO_PRINCIPAL, selecciones[SECTION_PLATO_PRINCIPAL]).orDash(),
                            guarnicion = MenuRepository.nombrePlato(SECTION_GUARNICIONES, selecciones[SECTION_GUARNICIONES]).orDash(),
                            postre = MenuRepository.nombrePlato(SECTION_POSTRES, selecciones[SECTION_POSTRES]).orDash()
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
    }

    fun obtenerReservasExportablesPorRango(
        desdeMillis: Long,
        hastaMillis: Long,
        onComplete: (Boolean, List<ReservaExportable>) -> Unit
    ) {
        val desdeNormalizado = normalizarFecha(desdeMillis)
        val hastaNormalizado = normalizarFecha(hastaMillis)
        if (desdeNormalizado > hastaNormalizado) {
            onComplete(false, emptyList())
            return
        }

        val (_, finDia) = dayBounds(hastaNormalizado)

        MenuRepository.cargarSecciones { _, _ ->
            firestore.collection(COLLECTION_RESERVAS)
                .whereGreaterThanOrEqualTo(FIELD_FECHA_MILLIS, desdeNormalizado)
                .whereLessThanOrEqualTo(FIELD_FECHA_MILLIS, finDia)
                .get()
                .addOnSuccessListener { snapshot ->
                    val rows = snapshot.documents.mapNotNull { doc ->
                        val fechaMillis = doc.getLong(FIELD_FECHA_MILLIS) ?: return@mapNotNull null
                        val selecciones = extraerSelecciones(doc)

                        ReservaExportable(
                            id = doc.id,
                            fechaMillis = normalizarFecha(fechaMillis),
                            empresa = doc.getString(FIELD_EMPRESA).orEmpty(),
                            apellido = doc.getString(FIELD_APELLIDO).orEmpty(),
                            nombre = doc.getString(FIELD_NOMBRE).orEmpty(),
                            userId = doc.getString(FIELD_USER_ID).orEmpty(),
                            platoPrincipal = MenuRepository.nombrePlato(SECTION_PLATO_PRINCIPAL, selecciones[SECTION_PLATO_PRINCIPAL]).orDash(),
                            guarnicion = MenuRepository.nombrePlato(SECTION_GUARNICIONES, selecciones[SECTION_GUARNICIONES]).orDash(),
                            postre = MenuRepository.nombrePlato(SECTION_POSTRES, selecciones[SECTION_POSTRES]).orDash(),
                            seleccionesMap = selecciones,
                            selecciones = MenuRepository.formatearSelecciones(selecciones)
                        )
                    }.sortedWith(
                        compareBy<ReservaExportable> { it.fechaMillis }
                            .thenBy { it.empresa.lowercase(Locale.getDefault()) }
                            .thenBy { it.apellido.lowercase(Locale.getDefault()) }
                            .thenBy { it.nombre.lowercase(Locale.getDefault()) }
                    )

                    onComplete(true, rows)
                }
                .addOnFailureListener {
                    onComplete(false, emptyList())
                }
        }
    }

    fun obtenerUltimaFechaArchivadaExportada(onComplete: (Boolean, Long?) -> Unit) {
        consultarUltimaOperacionArchivada(FIELD_FINALIZADO_EN) { ok, lastDate, error ->
            if (ok && lastDate != null) {
                onComplete(true, lastDate)
                return@consultarUltimaOperacionArchivada
            }

            consultarUltimaOperacionArchivada(FIELD_ARCHIVADO_EN) { fallbackOk, fallbackDate, fallbackError ->
                when {
                    fallbackOk -> onComplete(true, fallbackDate)
                    puedeDegradarConsultaUltimaOperacion(error) ||
                        puedeDegradarConsultaUltimaOperacion(fallbackError) -> onComplete(true, null)
                    else -> onComplete(false, null)
                }
            }
        }
    }

    fun iniciarOperacionArchivoReservas(
        rows: List<ReservaExportable>,
        desdeMillis: Long,
        hastaMillis: Long,
        archivoNombre: String,
        onComplete: (Boolean, ArchivoReservasOperacion?) -> Unit
    ) {
        val cantidadReservas = rows.size
        if (!puedeArchivarCantidad(cantidadReservas)) {
            onComplete(false, null)
            return
        }

        val currentUser = auth.currentUser
        val operadorUid = currentUser?.uid.orEmpty()
        if (operadorUid.isBlank()) {
            onComplete(false, null)
            return
        }

        val desdeNormalizado = normalizarFecha(desdeMillis)
        val hastaNormalizado = normalizarFecha(hastaMillis)
        val operationRef = firestore.collection(COLLECTION_RESERVAS_ARCHIVO_OPERACIONES).document()
        val payload = hashMapOf<String, Any>(
            FIELD_ESTADO to ESTADO_INICIADO,
            FIELD_OPERADOR_UID to operadorUid,
            FIELD_ARCHIVO_NOMBRE to archivoNombre,
            FIELD_RESERVA_IDS to rows.map { it.id },
            FIELD_DESDE_MILLIS to desdeNormalizado,
            FIELD_HASTA_MILLIS to hastaNormalizado,
            FIELD_CANTIDAD_RESERVAS to cantidadReservas,
            FIELD_INICIADO_EN to FieldValue.serverTimestamp()
        )
        currentUser?.email
            ?.takeIf { it.isNotBlank() }
            ?.let { payload[FIELD_OPERADOR_EMAIL] = it }

        operationRef
            .set(payload)
            .addOnSuccessListener {
                onComplete(
                    true,
                    ArchivoReservasOperacion(
                        id = operationRef.id,
                        archivoNombre = archivoNombre,
                        operadorUid = operadorUid,
                        operadorEmail = currentUser?.email?.takeIf { it.isNotBlank() }
                    )
                )
            }
            .addOnFailureListener {
                onComplete(false, null)
            }
    }

    fun marcarOperacionArchivoCsvGuardado(
        operacionId: String,
        onComplete: (Boolean) -> Unit
    ) {
        actualizarOperacionArchivoReservas(
            operacionId = operacionId,
            estado = ESTADO_CSV_GUARDADO,
            onComplete = onComplete
        )
    }

    fun cancelarOperacionArchivoReservas(
        operacionId: String,
        errorMessage: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        actualizarOperacionArchivoReservas(
            operacionId = operacionId,
            estado = ESTADO_CANCELADO,
            errorMessage = errorMessage,
            finalizada = true,
            onComplete = onComplete
        )
    }

    fun fallarOperacionArchivoReservas(
        operacionId: String,
        errorMessage: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        actualizarOperacionArchivoReservas(
            operacionId = operacionId,
            estado = ESTADO_ERROR,
            errorMessage = errorMessage,
            finalizada = true,
            onComplete = onComplete
        )
    }

    fun obtenerMaximoReservasArchivablesPorOperacion(): Int = MAX_ARCHIVE_OPERATION_ROWS

    fun puedeArchivarCantidad(cantidadReservas: Int): Boolean {
        return cantidadReservas in 1..MAX_ARCHIVE_OPERATION_ROWS
    }

    fun archivarReservasExportadas(
        rows: List<ReservaExportable>,
        desdeMillis: Long,
        hastaMillis: Long,
        operacionId: String,
        archivoNombre: String,
        operadorUid: String,
        operadorEmail: String?,
        onComplete: (Boolean, ArchivoReservasResult?) -> Unit
    ) {
        val cantidadReservas = rows.size
        if (!puedeArchivarCantidad(cantidadReservas)) {
            onComplete(false, null)
            return
        }

        val desdeNormalizado = normalizarFecha(desdeMillis)
        val hastaNormalizado = normalizarFecha(hastaMillis)
        val operationRef = firestore.collection(COLLECTION_RESERVAS_ARCHIVO_OPERACIONES).document(operacionId)
        val archivedAt = FieldValue.serverTimestamp()
        val batch = firestore.batch()

        rows.forEach { row ->
            val reservaRef = firestore.collection(COLLECTION_RESERVAS).document(row.id)
            val archivoRef = firestore.collection(COLLECTION_RESERVAS_ARCHIVO).document(row.id)
            val archivePayload = hashMapOf<String, Any>(
                FIELD_RESERVA_ID to row.id,
                FIELD_FECHA_MILLIS to normalizarFecha(row.fechaMillis),
                FIELD_EMPRESA to row.empresa,
                FIELD_APELLIDO to row.apellido,
                FIELD_NOMBRE to row.nombre,
                FIELD_USER_ID to row.userId,
                FIELD_SELECCIONES to row.seleccionesMap,
                FIELD_ARCHIVADO_EN to archivedAt,
                FIELD_ARCHIVO_OPERACION_ID to operationRef.id
            )

            batch.set(archivoRef, archivePayload)
            batch.delete(reservaRef)
        }

        val operationPayload = hashMapOf<String, Any>(
            FIELD_ESTADO to ESTADO_ARCHIVADO,
            FIELD_DESDE_MILLIS to desdeNormalizado,
            FIELD_HASTA_MILLIS to hastaNormalizado,
            FIELD_CANTIDAD_RESERVAS to cantidadReservas,
            FIELD_ARCHIVO_NOMBRE to archivoNombre,
            FIELD_RESERVA_IDS to rows.map { it.id },
            FIELD_ARCHIVADO_EN to archivedAt,
            FIELD_FINALIZADO_EN to archivedAt,
            FIELD_ERROR_MESSAGE to FieldValue.delete()
        )
        operadorUid
            .takeIf { it.isNotBlank() }
            ?.let { operationPayload[FIELD_OPERADOR_UID] = it }
        operadorEmail
            ?.takeIf { it.isNotBlank() }
            ?.let { operationPayload[FIELD_OPERADOR_EMAIL] = it }
        batch.set(operationRef, operationPayload, com.google.firebase.firestore.SetOptions.merge())

        batch.commit()
            .addOnSuccessListener {
                val archivedIds = rows.mapTo(hashSetOf()) { it.id }
                reservas.removeAll { reserva -> archivedIds.contains(reserva.id) }
                onComplete(
                    true,
                    ArchivoReservasResult(
                        operacionId = operacionId,
                        ultimaFechaArchivadaMillis = hastaNormalizado,
                        cantidadReservas = cantidadReservas
                    )
                )
            }
            .addOnFailureListener {
                onComplete(false, null)
            }
    }

    fun generarCsvReservas(rows: List<ReservaExportable>): String {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val builder = StringBuilder()
        builder.append("\uFEFF")
        builder.appendLine("reserva_id,fecha,empresa,apellido,nombre,user_id,plato_principal,guarnicion,postre,selecciones")
        rows.forEach { row ->
            builder.appendCsvRow(
                row.id,
                dateFormatter.format(Date(row.fechaMillis)),
                row.empresa,
                row.apellido,
                row.nombre,
                row.userId,
                row.platoPrincipal,
                row.guarnicion,
                row.postre,
                row.selecciones
            )
        }
        return builder.toString()
    }

    fun formatearSelecciones(selecciones: Map<String, String>): String {
        return MenuRepository.formatearSelecciones(sanitizeSelecciones(selecciones))
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

        val seccionesPorId = secciones.associateBy { it.id }
        val platoPrincipal = seccionesPorId[SECTION_PLATO_PRINCIPAL]
            ?.opciones
            ?.firstOrNull { plato ->
                plato.id == selecciones[SECTION_PLATO_PRINCIPAL]
            }
            ?: return false

        val guarnicionSeleccionada = selecciones[SECTION_GUARNICIONES]
        if (!platoPrincipal.guarnicion && !guarnicionSeleccionada.isNullOrBlank()) {
            return false
        }

        return selecciones.all { (tipoComida, plato) ->
            seccionesPorId[tipoComida]
                ?.opciones
                ?.any { opcion -> opcion.id == plato } == true
        }
    }

    private fun buildReservaDocumentId(uid: String, fechaMillis: Long): String {
        return "${uid.trim()}_${normalizarFecha(fechaMillis)}"
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

    private fun ventanaCreacionActual(): LongRange {
        return ventanaCreacionDesde(
            hoy = Calendar.getInstance(),
            config = BookingAvailabilityRepository.obtenerConfiguracionActual()
        )
    }

    private fun ventanaEdicionActual(): LongRange {
        return ventanaEdicionDesde(
            hoy = Calendar.getInstance(),
            config = BookingAvailabilityRepository.obtenerConfiguracionActual()
        )
    }

    private fun ventanaEdicionPermitidaActual(): LongRange {
        return ventanaEdicionPermitidaDesde(
            hoy = Calendar.getInstance(),
            config = BookingAvailabilityRepository.obtenerConfiguracionActual()
        )
    }

    private fun extraerSelecciones(doc: com.google.firebase.firestore.DocumentSnapshot): Map<String, String> {
        return (doc.get(FIELD_SELECCIONES) as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = v as? String ?: return@mapNotNull null
                key to value
            }
            ?.toMap()
            ?: emptyMap()
    }

    private fun dayBounds(fechaMillis: Long): Pair<Long, Long> {
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
        return inicioDia to finDia
    }

    private fun actualizarOperacionArchivoReservas(
        operacionId: String,
        estado: String,
        errorMessage: String? = null,
        finalizada: Boolean = false,
        onComplete: (Boolean) -> Unit
    ) {
        if (operacionId.isBlank()) {
            onComplete(false)
            return
        }

        val updates = hashMapOf<String, Any>(
            FIELD_ESTADO to estado
        )
        if (errorMessage.isNullOrBlank()) {
            updates[FIELD_ERROR_MESSAGE] = FieldValue.delete()
        } else {
            updates[FIELD_ERROR_MESSAGE] = errorMessage
        }
        if (finalizada) {
            updates[FIELD_FINALIZADO_EN] = FieldValue.serverTimestamp()
        }

        firestore.collection(COLLECTION_RESERVAS_ARCHIVO_OPERACIONES)
            .document(operacionId)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private fun consultarUltimaOperacionArchivada(
        orderByField: String,
        onComplete: (Boolean, Long?, Exception?) -> Unit
    ) {
        firestore.collection(COLLECTION_RESERVAS_ARCHIVO_OPERACIONES)
            .whereEqualTo(FIELD_ESTADO, ESTADO_ARCHIVADO)
            .orderBy(orderByField, com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val lastDate = snapshot.documents.firstOrNull()?.let(::extraerFechaOperacionArchivada)
                onComplete(true, lastDate, null)
            }
            .addOnFailureListener { error ->
                onComplete(false, null, error)
            }
    }

    private fun puedeDegradarConsultaUltimaOperacion(error: Exception?): Boolean {
        val firestoreError = error as? com.google.firebase.firestore.FirebaseFirestoreException ?: return false
        return firestoreError.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION
    }

    private fun extraerFechaOperacionArchivada(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): Long? {
        val value = doc.getLong(FIELD_HASTA_MILLIS)
            ?: doc.getTimestamp(FIELD_FINALIZADO_EN)?.toDate()?.time
            ?: doc.getTimestamp(FIELD_ARCHIVADO_EN)?.toDate()?.time
        return value?.let(::normalizarFecha)
    }

    private fun String?.orDash(): String = if (this.isNullOrBlank()) "-" else this
}

private fun StringBuilder.appendCsvRow(vararg values: String) {
    appendLine(values.joinToString(",") { value ->
        val escaped = value.replace("\"", "\"\"")
        "\"$escaped\""
    })
}

internal fun ventanaCreacionDesde(hoy: Calendar, config: BookingAvailabilityConfig): LongRange {
    val inicioReserva = inicioVentanaReserva(hoy, config)
    val finReserva = finVentanaReserva(inicioReserva, config.windowLengthDays)
    return inicioReserva.timeInMillis..finReserva.timeInMillis
}

internal fun ventanaEdicionDesde(hoy: Calendar, config: BookingAvailabilityConfig): LongRange {
    val inicioEdicion = (hoy.clone() as Calendar).clearTime()
    val finReserva = finVentanaReserva(inicioVentanaReserva(hoy, config), config.windowLengthDays)
    return inicioEdicion.timeInMillis..finReserva.timeInMillis
}

internal fun ventanaEdicionPermitidaDesde(hoy: Calendar, config: BookingAvailabilityConfig): LongRange {
    return ventanaCreacionDesde(hoy, config)
}

private fun inicioVentanaReserva(hoy: Calendar, config: BookingAvailabilityConfig): Calendar {
    return (hoy.clone() as Calendar).clearTime().apply {
        add(Calendar.DAY_OF_YEAR, sanitizeInitialDelayDays(config.initialDelayDays))
    }
}

private fun finVentanaReserva(inicioVentana: Calendar, windowLengthDays: Int): Calendar {
    return (inicioVentana.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        add(Calendar.DAY_OF_YEAR, sanitizeWindowLengthDays(windowLengthDays) - 1)
    }
}

private fun Calendar.clearTime(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
