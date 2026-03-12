package com.example.reservasapp

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MenuRepository {
    private const val COLLECTION_MENU = "menu"
    private const val COLLECTION_MENU_BY_DATE = "menu_por_fecha"
    private const val FIELD_SECTION = "section"
    private const val FIELD_NAME = "name"
    private const val FIELD_DETAIL = "detail"
    private const val FIELD_IMAGE_URL = "imageUrl"
    private const val FIELD_URL_IMAGE = "urlImage"
    private const val FIELD_GUARNICION = "guarnicion"
    private const val FIELD_DATE_KEY = "dateKey"
    private const val FIELD_ENABLED_DISH_KEYS = "enabledDishKeys"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val secciones = mutableListOf<MenuSection>()

    private val seccionesBase = listOf("Plato principal", "Guarniciones", "Postres")

    fun seccionesPermitidas(): List<String> = seccionesBase

    fun obtenerSeccionesCache(): List<MenuSection> = secciones.map {
        MenuSection(it.nombre, it.opciones.toMutableList())
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
                seccionesBase.forEach { agrupadas[it] = mutableListOf() }

                snapshot.documents.forEach { doc ->
                    val section = doc.getString(FIELD_SECTION)?.trim().orEmpty()
                    val name = doc.getString(FIELD_NAME)?.trim().orEmpty()
                    val detail = doc.getString(FIELD_DETAIL)?.trim().orEmpty()
                    val imageUrl = doc.getString(FIELD_URL_IMAGE)?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: doc.getString(FIELD_IMAGE_URL)?.trim().orEmpty()
                    val guarnicion = doc.getBoolean(FIELD_GUARNICION) ?: false

                    if (section.isBlank() || name.isBlank()) return@forEach
                    if (!agrupadas.containsKey(section)) return@forEach

                    agrupadas[section]?.add(
                        MenuDish(
                            nombre = name,
                            detalle = detail,
                            imageUrl = imageUrl,
                            guarnicion = guarnicion
                        )
                    )
                }

                val nuevasSecciones = agrupadas.map { (nombre, opciones) ->
                    MenuSection(nombre, opciones)
                }

                if (fechaMillis == null) {
                    secciones.clear()
                    secciones.addAll(nuevasSecciones)
                    onComplete(true, obtenerSeccionesCache())
                    return@addOnSuccessListener
                }

                obtenerClavesPlatosHabilitados(fechaMillis) { ok, clavesHabilitadas ->
                    if (!ok) {
                        onComplete(false, obtenerSeccionesCache())
                        return@obtenerClavesPlatosHabilitados
                    }

                    val seccionesFiltradas = if (clavesHabilitadas == null) {
                        nuevasSecciones
                    } else {
                        nuevasSecciones.map { section ->
                            val opcionesFiltradas = section.opciones.filter { dish ->
                                val clave = clavePlato(section.nombre, dish.nombre)
                                clavesHabilitadas.contains(clave)
                            }.toMutableList()
                            MenuSection(section.nombre, opcionesFiltradas)
                        }
                    }

                    secciones.clear()
                    secciones.addAll(seccionesFiltradas)
                    onComplete(true, obtenerSeccionesCache())
                }
            }
            .addOnFailureListener {
                onComplete(false, obtenerSeccionesCache())
            }
    }

    fun clavePlato(seccion: String, nombrePlato: String): String {
        return "${seccion.trim().lowercase(Locale.ROOT)}|${nombrePlato.trim().lowercase(Locale.ROOT)}"
    }

    fun obtenerClavesPlatosHabilitados(
        fechaMillis: Long,
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

                val keys = (doc.get(FIELD_ENABLED_DISH_KEYS) as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    ?: emptySet()
                onComplete(true, keys)
            }
            .addOnFailureListener {
                onComplete(false, null)
            }
    }

    fun guardarPlatosHabilitadosParaFecha(
        fechaMillis: Long,
        dishKeys: Set<String>,
        onComplete: (Boolean) -> Unit
    ) {
        val dateKey = dateKey(fechaMillis)
        val payload = mapOf(
            FIELD_DATE_KEY to dateKey,
            FIELD_ENABLED_DISH_KEYS to dishKeys.toList()
        )

        firestore.collection(COLLECTION_MENU_BY_DATE)
            .document(dateKey)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    private fun dateKey(fechaMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date(fechaMillis))
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
        val nombreNormalizado = nombre.trim()
        if (!seccionesBase.contains(seccionNormalizada) || nombreNormalizado.isBlank()) {
            onComplete(false)
            return
        }

        val payload = mapOf(
            FIELD_SECTION to seccionNormalizada,
            FIELD_NAME to nombreNormalizado,
            FIELD_DETAIL to detalle.trim(),
            FIELD_URL_IMAGE to imageUrl.trim(),
            FIELD_IMAGE_URL to imageUrl.trim(),
            FIELD_GUARNICION to guarnicion
        )

        firestore.collection(COLLECTION_MENU)
            .add(payload)
            .addOnSuccessListener {
                cargarSecciones { ok, _ -> onComplete(ok) }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
}
