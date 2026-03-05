package com.example.reservasapp

import java.util.Locale

object ComidaImageRepository {

    private val imagenesComida = mapOf(
        normalizarNombre("Pollo al horno") to R.drawable.pollo_horno,
        normalizarNombre("Milanesa Napolitana") to R.drawable.milanesa_napolitana,
        normalizarNombre("Pure de papas") to R.drawable.pure_papas,
        normalizarNombre("Papas al horno") to R.drawable.papas_horno,
        normalizarNombre("Flan") to R.drawable.flan,
        normalizarNombre("Gelatina") to R.drawable.gelatina
    )

    fun obtenerImagenComida(nombre: String): Int {
        val clave = normalizarNombre(nombre)
        return imagenesComida[clave] ?: R.drawable.placeholder_comida
    }

    private fun normalizarNombre(nombre: String): String {
        return nombre.trim().lowercase(Locale.ROOT)
    }
}
