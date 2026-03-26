package com.example.reservasapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfirmacionReservaHelpersTest {

    @Test
    fun extraerSeleccionResuelveAliasesDelResumenDeConfirmacion() {
        val selecciones = linkedMapOf(
            "Plato Principal del dia" to "Milanesa napolitana",
            "GUARNICIONES disponibles" to "Pure de papas",
            "Postres" to "Flan casero"
        )

        assertEquals("Milanesa napolitana", extraerSeleccion(selecciones, "plato", "principal"))
        assertEquals("Pure de papas", extraerSeleccion(selecciones, "guarn"))
        assertEquals("Flan casero", extraerSeleccion(selecciones, "postre"))
    }

    @Test
    fun extraerSeleccionDevuelveNullCuandoNoEncuentraAliasCompatibles() {
        val selecciones = mapOf("Bebidas" to "Agua")

        assertNull(extraerSeleccion(selecciones, "postre"))
    }

    @Test
    fun normalizarNombreUnificaEspaciosYMayusculasParaLookupDeImagenes() {
        assertEquals("milanesa napolitana", normalizarNombre("  Milanesa   Napolitana  "))
    }
}
