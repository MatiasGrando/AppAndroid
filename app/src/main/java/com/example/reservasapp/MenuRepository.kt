package com.example.reservasapp

object MenuRepository {
    private val secciones = mutableListOf(
        MenuSection(
            "Plato principal",
            mutableListOf("Pollo al horno", "Milanesa Napolitana", "Empanadas")
        ),
        MenuSection(
            "Guarniciones",
            mutableListOf("Pure de papas", "Papas al horno", "Ensalada mixta")
        ),
        MenuSection(
            "Postres",
            mutableListOf("Flan", "Gelatina", "Alfajor", "Fruta")
        )
    )

    fun obtenerSecciones(): List<MenuSection> = secciones.map {
        MenuSection(it.nombre, it.opciones.toMutableList())
    }

    fun obtenerOpcionesPorSeccion(nombre: String): List<String> {
        return secciones.firstOrNull { it.nombre == nombre }?.opciones?.toList().orEmpty()
    }

    fun actualizarOpciones(nombre: String, nuevasOpciones: List<String>) {
        val section = secciones.firstOrNull { it.nombre == nombre } ?: return
        section.opciones.clear()
        section.opciones.addAll(nuevasOpciones)
    }

    fun agregarSeccion(nombre: String, opciones: List<String>) {
        if (nombre.isBlank()) return
        if (secciones.any { it.nombre.equals(nombre.trim(), ignoreCase = true) }) return
        secciones.add(MenuSection(nombre.trim(), opciones.toMutableList()))
    }
}
