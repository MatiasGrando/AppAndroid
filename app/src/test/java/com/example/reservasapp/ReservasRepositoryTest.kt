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
    fun ventanaCreacionRespetaMargenInicialYLongitudTotal() {
        val hoy = fechaBase()
        val config = BookingAvailabilityConfig(
            enabledWeekdays = DAY_ORDER.toSet(),
            initialDelayDays = 2,
            windowLengthDays = 10
        )

        val ventana = ventanaCreacionDesde(hoy, config)

        assertFalse(fechaConOffsetDesde(hoy, days = 1) in ventana)
        assertTrue(fechaConOffsetDesde(hoy, days = 2) in ventana)
        assertTrue(fechaConOffsetDesde(hoy, days = 11) in ventana)
        assertFalse(fechaConOffsetDesde(hoy, days = 12) in ventana)
    }

    @Test
    fun ventanaEdicionMantieneFechasExistentesAntesDelMargenInicial() {
        val hoy = fechaBase()
        val config = BookingAvailabilityConfig(
            enabledWeekdays = DAY_ORDER.toSet(),
            initialDelayDays = 3,
            windowLengthDays = 5
        )

        val ventana = ventanaEdicionDesde(hoy, config)

        assertTrue(fechaConOffsetDesde(hoy, days = 0) in ventana)
        assertTrue(fechaConOffsetDesde(hoy, days = 2) in ventana)
        assertTrue(fechaConOffsetDesde(hoy, days = 7) in ventana)
        assertFalse(fechaConOffsetDesde(hoy, days = 8) in ventana)
    }

    @Test
    fun ventanaEdicionPermitidaRespetaMargenInicialYLongitudTotal() {
        val hoy = fechaBase()
        val config = BookingAvailabilityConfig(
            enabledWeekdays = DAY_ORDER.toSet(),
            initialDelayDays = 3,
            windowLengthDays = 5
        )

        val ventana = ventanaEdicionPermitidaDesde(hoy, config)

        assertFalse(fechaConOffsetDesde(hoy, days = 0) in ventana)
        assertFalse(fechaConOffsetDesde(hoy, days = 2) in ventana)
        assertTrue(fechaConOffsetDesde(hoy, days = 3) in ventana)
        assertTrue(fechaConOffsetDesde(hoy, days = 7) in ventana)
        assertFalse(fechaConOffsetDesde(hoy, days = 8) in ventana)
    }

    @Test
    fun formatearSeleccionesLimpiaEspaciosYDescartaEntradasVacias() {
        val selecciones = linkedMapOf(
            " ${MenuIdentity.SECTION_MAIN} " to " dish-main-1 ",
            MenuIdentity.SECTION_SIDE to "   ",
            "   " to "No deberia entrar",
            MenuIdentity.SECTION_DESSERT to " dish-dessert-1 "
        )

        val resultado = ReservasRepository.formatearSelecciones(selecciones)

        assertEquals("Plato principal: dish-main-1 | Postres: dish-dessert-1", resultado)
    }

    @Test
    fun seleccionesSonValidasParaMenuAceptaSeleccionesSanitizadas() {
        val seleccionesCrudas = linkedMapOf(
            " ${MenuIdentity.SECTION_MAIN} " to " dish-main-1 ",
            " ${MenuIdentity.SECTION_SIDE} " to " dish-side-1 ",
            MenuIdentity.SECTION_DESSERT to " dish-dessert-1 "
        )
        val selecciones = ReservasRepository.sanitizeSelecciones(seleccionesCrudas)

        assertTrue(ReservasRepository.seleccionesSonValidasParaMenu(menuBase(), selecciones))
    }

    @Test
    fun seleccionesSonValidasParaMenuRechazaGuarnicionSiElPrincipalNoLaPermite() {
        val selecciones = linkedMapOf(
            MenuIdentity.SECTION_MAIN to "dish-main-2",
            MenuIdentity.SECTION_SIDE to "dish-side-1"
        )

        assertFalse(ReservasRepository.seleccionesSonValidasParaMenu(menuBase(), selecciones))
    }

    @Test
    fun seleccionesSonValidasParaMenuRechazaCuandoFaltaElPrincipal() {
        val selecciones = linkedMapOf(
            MenuIdentity.SECTION_SIDE to "dish-side-1",
            MenuIdentity.SECTION_DESSERT to "dish-dessert-1"
        )

        assertFalse(ReservasRepository.seleccionesSonValidasParaMenu(menuBase(), selecciones))
    }

    private fun menuBase(): List<MenuSection> {
        return listOf(
            MenuSection(
                id = MenuIdentity.SECTION_MAIN,
                nombre = "Plato principal",
                opciones = mutableListOf(
                    MenuDish(
                        id = "dish-main-1",
                        nombre = "Milanesa napolitana",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = true
                    ),
                    MenuDish(
                        id = "dish-main-2",
                        nombre = "Pasta del dia",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            ),
            MenuSection(
                id = MenuIdentity.SECTION_SIDE,
                nombre = "Guarniciones",
                opciones = mutableListOf(
                    MenuDish(
                        id = "dish-side-1",
                        nombre = "Pure de papas",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            ),
            MenuSection(
                id = MenuIdentity.SECTION_DESSERT,
                nombre = "Postres",
                opciones = mutableListOf(
                    MenuDish(
                        id = "dish-dessert-1",
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

    private fun fechaBase(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun fechaConOffsetDesde(base: Calendar, days: Int): Long {
        return (base.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }
}
