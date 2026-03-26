package com.example.reservasapp

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservasRepositoryTest {

    @Test
    fun esFechaReservableAceptaHoyYElUltimoDiaPermitido() {
        assertTrue(ReservasRepository.esFechaReservable(fechaConOffset(days = 0)))
        assertTrue(ReservasRepository.esFechaReservable(fechaConOffset(days = 6)))
    }

    @Test
    fun esFechaReservableRechazaFueraDeLaVentanaActual() {
        assertFalse(ReservasRepository.esFechaReservable(fechaConOffset(days = -1)))
        assertFalse(ReservasRepository.esFechaReservable(fechaConOffset(days = 7)))
    }

    @Test
    fun formatearSeleccionesLimpiaEspaciosYDescartaEntradasVacias() {
        val selecciones = linkedMapOf(
            " Plato principal " to " Milanesa napolitana ",
            "Guarniciones" to "   ",
            "   " to "No deberia entrar",
            "Postres" to " Flan casero "
        )

        val resultado = ReservasRepository.formatearSelecciones(selecciones)

        assertEquals("Plato principal: Milanesa napolitana | Postres: Flan casero", resultado)
    }

    @Test
    fun seleccionesSonValidasParaMenuAceptaClavesYValoresNormalizados() {
        val selecciones = linkedMapOf(
            " plato principal " to "  milanesa napolitana ",
            " Guarniciones " to " Pure de papas ",
            "Postres" to " flan casero "
        )

        assertTrue(ReservasRepository.seleccionesSonValidasParaMenu(menuBase(), selecciones))
    }

    @Test
    fun seleccionesSonValidasParaMenuRechazaGuarnicionSiElPrincipalNoLaPermite() {
        val selecciones = linkedMapOf(
            "Plato principal" to "Pasta del dia",
            "Guarniciones" to "Pure de papas"
        )

        assertFalse(ReservasRepository.seleccionesSonValidasParaMenu(menuBase(), selecciones))
    }

    @Test
    fun seleccionesSonValidasParaMenuRechazaCuandoFaltaElPrincipal() {
        val selecciones = linkedMapOf(
            "Guarniciones" to "Pure de papas",
            "Postres" to "Flan casero"
        )

        assertFalse(ReservasRepository.seleccionesSonValidasParaMenu(menuBase(), selecciones))
    }

    private fun menuBase(): List<MenuSection> {
        return listOf(
            MenuSection(
                nombre = "Plato principal",
                opciones = mutableListOf(
                    MenuDish(
                        nombre = "Milanesa napolitana",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = true
                    ),
                    MenuDish(
                        nombre = "Pasta del dia",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            ),
            MenuSection(
                nombre = "Guarniciones",
                opciones = mutableListOf(
                    MenuDish(
                        nombre = "Pure de papas",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            ),
            MenuSection(
                nombre = "Postres",
                opciones = mutableListOf(
                    MenuDish(
                        nombre = "Flan casero",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            )
        )
    }

    private fun fechaConOffset(days: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }
}
