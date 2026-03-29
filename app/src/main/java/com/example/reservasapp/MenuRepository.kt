package com.example.reservasapp

import com.example.reservasapp.firebase.FirebaseProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MenuRepository {
    private const val COLLECTION_MENU = "menu"
    private const val COLLECTION_MENU_BY_DATE = "menu_por_fecha"
    private const val MAX_BATCH_WRITES = 450
    private const val FIELD_SECTION = "section"
    private const val FIELD_SECTION_ID = "sectionId"
    private const val FIELD_NAME = "name"
    private const val FIELD_DISH_ID = "dishId"
    private const val FIELD_DETAIL = "detail"
    private const val FIELD_URL_IMAGE = "urlImage"
    private const val FIELD_GUARNICION = "guarnicion"
    private const val FIELD_DATE_KEY = "dateKey"
    private const val FIELD_ENABLED_DISH_IDS = "enabledDishIds"
    private const val FIELD_ENABLED_DISH_KEYS = "enabledDishKeys"

    private val firestore by lazy { FirebaseProvider.firestore() }
    private val secciones = mutableListOf<MenuSection>()
    private var isLegacyMenuByDateBackfillRunning = false
    private var isLegacyMenuByDateBackfillCompleted = false

    fun seccionesPermitidas(): List<String> = MenuIdentity.sectionDefinitions().map { it.displayName }

    fun obtenerSeccionesCache(): List<MenuSection> = secciones.map { section ->
        MenuSection(section.id, section.nombre, section.opciones.toMutableList())
    }

    fun obtenerSecciones(): List<MenuSection> = obtenerSeccionesCache()

    fun cargarSecciones(onComplete: (Boolean, List<MenuSection>) -> Unit) {
        cargarSecciones(null, onComplete)
    }

    fun cargarSecciones(fechaMillis: Long?, onComplete: (Boolean, List<MenuSection>) -> Unit) {
        firestore.collection(COLLECTION_MENU)
            .get()
            .addOnSuccessListener { snapshot ->
                val agrupadas = linkedMapOf<String, MutableList<MenuDish>>()
                MenuIdentity.sectionDefinitions().forEach { agrupadas[it.id] = mutableListOf() }

                snapshot.documents.forEach { doc ->
                    val sectionId = MenuIdentity.normalizeSectionId(
                        rawSectionId = doc.getString(FIELD_SECTION_ID),
                        rawSectionName = doc.getString(FIELD_SECTION)
                    ) ?: return@forEach
                    val name = doc.getString(FIELD_NAME)?.trim().orEmpty()
                    val dishId = doc.getString(FIELD_DISH_ID)?.trim().orEmpty().ifBlank { doc.id }
                    val detail = doc.getString(FIELD_DETAIL)?.trim().orEmpty()
                    val imageUrl = doc.getString(FIELD_URL_IMAGE)?.trim().orEmpty()
                    val guarnicion = doc.getBoolean(FIELD_GUARNICION) ?: false

                    if (name.isBlank()) return@forEach

                    agrupadas[sectionId]?.add(
                        MenuDish(
                            id = dishId,
                            nombre = name,
                            detalle = detail,
                            imageUrl = imageUrl,
                            guarnicion = guarnicion
                        )
                    )
                }

                val nuevasSecciones = MenuIdentity.sectionDefinitions().map { definition ->
                    MenuSection(
                        id = definition.id,
                        nombre = definition.displayName,
                        opciones = agrupadas[definition.id].orEmpty().toMutableList()
                    )
                }

                migrateLegacyMenuByDateDocumentsIfNeeded(nuevasSecciones)

                if (fechaMillis == null) {
                    persistirSecciones(nuevasSecciones, onComplete)
                    return@addOnSuccessListener
                }

                obtenerIdsPlatosHabilitados(fechaMillis, nuevasSecciones) { ok, dishIds ->
                    if (!ok) {
                        onComplete(false, obtenerSeccionesCache())
                        return@obtenerIdsPlatosHabilitados
                    }

                    val seccionesFiltradas = if (dishIds == null) {
                        nuevasSecciones
                    } else {
                        nuevasSecciones.map { section ->
                            MenuSection(
                                id = section.id,
                                nombre = section.nombre,
                                opciones = section.opciones.filter { dish -> dish.id in dishIds }.toMutableList()
                            )
                        }
                    }

                    persistirSecciones(seccionesFiltradas, onComplete)
                }
            }
            .addOnFailureListener {
                onComplete(false, obtenerSeccionesCache())
            }
    }

    fun obtenerIdsPlatosHabilitados(
        fechaMillis: Long,
        availableSections: List<MenuSection> = obtenerSeccionesCache(),
        onComplete: (Boolean, Set<String>?) -> Unit
    ) {
        val dateKey = dateKey(fechaMillis)
        firestore.collection(COLLECTION_MENU_BY_DATE)
            .document(dateKey)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onComplete(true, null)
                    return@addOnSuccessListener
                }

                val ids = (doc.get(FIELD_ENABLED_DISH_IDS) as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()

                if (!ids.isNullOrEmpty()) {
                    onComplete(true, ids)
                    return@addOnSuccessListener
                }

                val fallbackKeys = (doc.get(FIELD_ENABLED_DISH_KEYS) as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    .orEmpty()
                if (fallbackKeys.isEmpty()) {
                    onComplete(true, emptySet())
                    return@addOnSuccessListener
                }

                val mappedIds = availableSections
                    .flatMap { section ->
                        section.opciones.mapNotNull { dish ->
                            dish.id.takeIf { clavePlato(section.nombre, dish.nombre) in fallbackKeys }
                        }
                    }
                    .toSet()
                onComplete(true, mappedIds)
            }
            .addOnFailureListener {
                onComplete(false, null)
            }
    }

    fun guardarPlatosHabilitadosParaFecha(
        fechaMillis: Long,
        dishIds: Set<String>,
        onComplete: (Boolean) -> Unit
    ) {
        val dateKey = dateKey(fechaMillis)
        val payload = mapOf(
            FIELD_DATE_KEY to dateKey,
            FIELD_ENABLED_DISH_IDS to dishIds.toList()
        )

        firestore.collection(COLLECTION_MENU_BY_DATE)
            .document(dateKey)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun agregarPlato(
        seccion: String,
        nombre: String,
        detalle: String,
        imageUrl: String,
        guarnicion: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val seccionNormalizada = seccion.trim()
        val nombreNormalizado = MenuTextNormalizer.normalizeDishName(nombre)
        val detalleNormalizado = MenuTextNormalizer.normalizeDishDescription(detalle)
        val sectionId = MenuIdentity.normalizeSectionId(rawSectionId = null, rawSectionName = seccionNormalizada)
        if (sectionId == null || nombreNormalizado.isBlank()) {
            onComplete(false)
            return
        }

        val payload = mutableMapOf<String, Any>(
            FIELD_SECTION to MenuIdentity.sectionDisplayName(sectionId),
            FIELD_SECTION_ID to sectionId,
            FIELD_NAME to nombreNormalizado,
            FIELD_DETAIL to detalleNormalizado,
            FIELD_URL_IMAGE to imageUrl.trim(),
            FIELD_GUARNICION to guarnicion
        )

        firestore.collection(COLLECTION_MENU)
            .add(payload)
            .addOnSuccessListener { docRef ->
                docRef.set(mapOf(FIELD_DISH_ID to docRef.id), SetOptions.merge())
                    .addOnSuccessListener {
                        cargarSecciones { ok, _ -> onComplete(ok) }
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun actualizarPlato(
        dishId: String,
        seccion: String,
        nombre: String,
        detalle: String,
        imageUrl: String,
        guarnicion: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val dishIdNormalizado = dishId.trim()
        val seccionNormalizada = seccion.trim()
        val nombreNormalizado = MenuTextNormalizer.normalizeDishName(nombre)
        val detalleNormalizado = MenuTextNormalizer.normalizeDishDescription(detalle)
        val sectionId = MenuIdentity.normalizeSectionId(rawSectionId = null, rawSectionName = seccionNormalizada)
        if (dishIdNormalizado.isBlank() || sectionId == null || nombreNormalizado.isBlank()) {
            onComplete(false)
            return
        }

        val payload = mutableMapOf<String, Any>(
            FIELD_DISH_ID to dishIdNormalizado,
            FIELD_SECTION to MenuIdentity.sectionDisplayName(sectionId),
            FIELD_SECTION_ID to sectionId,
            FIELD_NAME to nombreNormalizado,
            FIELD_DETAIL to detalleNormalizado,
            FIELD_URL_IMAGE to imageUrl.trim(),
            FIELD_GUARNICION to guarnicion
        )

        firestore.collection(COLLECTION_MENU)
            .whereEqualTo(FIELD_DISH_ID, dishIdNormalizado)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val documentId = snapshot.documents.firstOrNull()?.id ?: dishIdNormalizado
                firestore.collection(COLLECTION_MENU)
                    .document(documentId)
                    .set(payload, SetOptions.merge())
                    .addOnSuccessListener {
                        cargarSecciones { ok, _ -> onComplete(ok) }
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun formatearSelecciones(selecciones: Map<String, String>): String {
        return resolveSelections(selecciones)
            .entries
            .joinToString(" | ") { "${it.key}: ${it.value}" }
    }

    fun resolveSelections(selecciones: Map<String, String>): LinkedHashMap<String, String> {
        val seccionesPorId = obtenerSeccionesCache().associateBy { it.id }
        val orden = MenuIdentity.sectionDefinitions().map { it.id }
        val resultado = linkedMapOf<String, String>()

        orden.forEach { sectionId ->
            val dishId = selecciones[sectionId].orEmpty()
            if (dishId.isBlank()) return@forEach

            val section = seccionesPorId[sectionId]
            val visibleSectionName = section?.nombre ?: MenuIdentity.sectionDisplayName(sectionId)
            val visibleDishName = section?.opciones?.firstOrNull { it.id == dishId }?.nombre ?: dishId
            resultado[visibleSectionName] = visibleDishName
        }

        return resultado
    }

    fun nombrePlato(sectionId: String, dishId: String?): String? {
        val dish = findDish(sectionId, dishId)
        return when {
            dish != null -> dish.nombre
            dishId.isNullOrBlank() -> null
            else -> dishId
        }
    }

    fun imageUrlPorDishId(dishId: String?): String {
        if (dishId.isNullOrBlank()) return ""
        return obtenerSeccionesCache()
            .flatMap { it.opciones }
            .firstOrNull { it.id == dishId }
            ?.imageUrl
            .orEmpty()
    }

    fun findDish(sectionId: String, dishId: String?): MenuDish? {
        if (dishId.isNullOrBlank()) return null
        return obtenerSeccionesCache()
            .firstOrNull { it.id == sectionId }
            ?.opciones
            ?.firstOrNull { it.id == dishId }
    }

    fun clavePlato(seccion: String, nombrePlato: String): String {
        return "${seccion.trim().lowercase(Locale.ROOT)}|${nombrePlato.trim().lowercase(Locale.ROOT)}"
    }

    internal fun legacyDishIdsByKey(availableSections: List<MenuSection>): Map<String, String> {
        return availableSections
            .flatMap { section ->
                section.opciones.map { dish ->
                    clavePlato(section.nombre, dish.nombre) to dish.id
                }
            }
            .toMap()
    }

    internal fun planLegacyMenuByDateBackfill(
        documentId: String,
        documentData: Map<String, Any>?,
        dishIdsByLegacyKey: Map<String, String>
    ): Map<String, Any>? {
        if (documentData == null) return null

        val legacyKeys = stringSetFromField(documentData[FIELD_ENABLED_DISH_KEYS])
        if (legacyKeys.isEmpty()) return null

        val existingIds = stringSetFromField(documentData[FIELD_ENABLED_DISH_IDS])
        val resolvedIds = legacyKeys.mapNotNull { legacyKey -> dishIdsByLegacyKey[legacyKey] }.toSet()
        val normalizedIds = when {
            resolvedIds.size == legacyKeys.size -> resolvedIds
            resolvedIds.isNotEmpty() -> existingIds + resolvedIds
            else -> existingIds
        }
        val currentDateKey = documentData[FIELD_DATE_KEY] as? String
        val shouldWriteDateKey = currentDateKey.isNullOrBlank()

        if (normalizedIds.isEmpty()) {
            return null
        }

        val shouldWriteIds = normalizedIds != existingIds
        if (!shouldWriteIds && !shouldWriteDateKey) {
            return null
        }

        val payload = mutableMapOf<String, Any>(
            FIELD_ENABLED_DISH_IDS to normalizedIds.toList().sorted()
        )
        if (shouldWriteDateKey) {
            payload[FIELD_DATE_KEY] = documentId
        }
        return payload
    }

    private fun persistirSecciones(
        nuevasSecciones: List<MenuSection>,
        onComplete: (Boolean, List<MenuSection>) -> Unit
    ) {
        secciones.clear()
        secciones.addAll(nuevasSecciones)
        onComplete(true, obtenerSeccionesCache())
    }

    private fun dateKey(fechaMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(fechaMillis))
    }

    private fun migrateLegacyMenuByDateDocumentsIfNeeded(availableSections: List<MenuSection>) {
        if (isLegacyMenuByDateBackfillCompleted || isLegacyMenuByDateBackfillRunning) {
            return
        }

        val dishIdsByLegacyKey = legacyDishIdsByKey(availableSections)
        if (dishIdsByLegacyKey.isEmpty()) {
            return
        }

        isLegacyMenuByDateBackfillRunning = true
        firestore.collection(COLLECTION_MENU_BY_DATE)
            .get()
            .addOnSuccessListener { snapshot ->
                val updates = snapshot.documents.mapNotNull { document ->
                    val payload = planLegacyMenuByDateBackfill(
                        documentId = document.id,
                        documentData = document.data,
                        dishIdsByLegacyKey = dishIdsByLegacyKey
                    ) ?: return@mapNotNull null
                    document.id to payload
                }

                if (updates.isEmpty()) {
                    isLegacyMenuByDateBackfillRunning = false
                    isLegacyMenuByDateBackfillCompleted = true
                    return@addOnSuccessListener
                }

                commitLegacyMenuByDateBackfill(updates, startIndex = 0)
            }
            .addOnFailureListener {
                isLegacyMenuByDateBackfillRunning = false
            }
    }

    private fun commitLegacyMenuByDateBackfill(
        updates: List<Pair<String, Map<String, Any>>>,
        startIndex: Int
    ) {
        if (startIndex >= updates.size) {
            isLegacyMenuByDateBackfillRunning = false
            isLegacyMenuByDateBackfillCompleted = true
            return
        }

        val endExclusive = minOf(startIndex + MAX_BATCH_WRITES, updates.size)
        val batch = firestore.batch()
        updates.subList(startIndex, endExclusive).forEach { (documentId, payload) ->
            batch.set(
                firestore.collection(COLLECTION_MENU_BY_DATE).document(documentId),
                payload,
                SetOptions.merge()
            )
        }

        batch.commit()
            .addOnSuccessListener {
                commitLegacyMenuByDateBackfill(updates, endExclusive)
            }
            .addOnFailureListener {
                isLegacyMenuByDateBackfillRunning = false
            }
    }

    private fun stringSetFromField(value: Any?): Set<String> {
        return (value as? List<*>)
            ?.mapNotNull { (it as? String)?.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
    }
}
