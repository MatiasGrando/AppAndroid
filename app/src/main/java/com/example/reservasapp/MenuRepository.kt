package com.example.reservasapp

import com.google.firebase.firestore.FirebaseFirestore

object MenuRepository {
    private const val COLLECTION_MENU = "menu"
    private const val FIELD_SECTION = "section"
    private const val FIELD_NAME = "name"
    private const val FIELD_DETAIL = "detail"
    private const val FIELD_IMAGE_URL = "imageUrl"
    private const val FIELD_GUARNICION = "guarnicion"

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val secciones = mutableListOf<MenuSection>()

    private val seccionesBase = listOf("Plato principal", "Guarniciones", "Postres")

    fun seccionesPermitidas(): List<String> = seccionesBase

    fun obtenerSeccionesCache(): List<MenuSection> = secciones.map {
        MenuSection(it.nombre, it.opciones.toMutableList())
    }

    fun obtenerSecciones(): List<MenuSection> = obtenerSeccionesCache()

    fun cargarSecciones(onComplete: (Boolean, List<MenuSection>) -> Unit) {
        firestore.collection(COLLECTION_MENU)
            .get()
            .addOnSuccessListener { snapshot ->
                val agrupadas = linkedMapOf<String, MutableList<MenuDish>>()
                seccionesBase.forEach { agrupadas[it] = mutableListOf() }

                snapshot.documents.forEach { doc ->
                    val section = doc.getString(FIELD_SECTION)?.trim().orEmpty()
                    val name = doc.getString(FIELD_NAME)?.trim().orEmpty()
                    val detail = doc.getString(FIELD_DETAIL)?.trim().orEmpty()
                    val imageUrl = doc.getString(FIELD_IMAGE_URL)?.trim().orEmpty()
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

                secciones.clear()
                secciones.addAll(nuevasSecciones)
                onComplete(true, obtenerSeccionesCache())
            }
            .addOnFailureListener {
                onComplete(false, obtenerSeccionesCache())
            }
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
