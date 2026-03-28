package com.example.reservasapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmacionReservaRequestResolverTest {

    @Test
    fun createRequestRejectsEmptyPendingSelectionsWhenExtraWasSent() {
        val resolution = resolveConfirmacionReservaRequest(
            rawRequest = baseRawRequest(
                hasSeleccionesPendientesExtra = true,
                seleccionesPendientesRaw = linkedMapOf(MenuIdentity.SECTION_MAIN to "   ")
            ),
            sanitizeSelecciones = { emptyMap() },
            canCreateReservationDate = { true }
        )

        assertEquals(
            ConfirmacionReservaRequestResolution.Invalid(R.string.error_guardar_reserva),
            resolution
        )
    }

    @Test
    fun createRequestRejectsNonReservableDateBeforeRendering() {
        val resolution = resolveConfirmacionReservaRequest(
            rawRequest = baseRawRequest(fechaMillis = 0L),
            canCreateReservationDate = { false }
        )

        assertEquals(
            ConfirmacionReservaRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible),
            resolution
        )
    }

    @Test
    fun createRequestKeepsSanitizedPendingSelections() {
        val resolution = resolveConfirmacionReservaRequest(
            rawRequest = baseRawRequest(
                hasSeleccionesPendientesExtra = true,
                seleccionesPendientesRaw = linkedMapOf(
                    " ${MenuIdentity.SECTION_MAIN} " to " dish-main ",
                    MenuIdentity.SECTION_SIDE to " dish-side "
                )
            ),
            sanitizeSelecciones = { raw -> raw.mapKeys { it.key.trim() }.mapValues { it.value.trim() } },
            canCreateReservationDate = { true }
        )

        val request = (resolution as ConfirmacionReservaRequestResolution.Valid).request

        assertEquals(1_717_171_717_000L, request.fechaMillis)
        assertEquals(
            linkedMapOf(
                MenuIdentity.SECTION_MAIN to "dish-main",
                MenuIdentity.SECTION_SIDE to "dish-side"
            ),
            request.seleccionesPendientes
        )
    }

    @Test
    fun editRequestRejectsMissingReservation() {
        val resolution = resolveConfirmacionReservaRequest(
            rawRequest = baseRawRequest(esEdicion = true, reservaId = "reserva-inexistente"),
            reservaById = { null },
            canEditReservationDate = { true }
        )

        assertEquals(
            ConfirmacionReservaRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible),
            resolution
        )
    }

    @Test
    fun editRequestUsesReservationDateFromRepository() {
        val reserva = Reserva(
            id = "reserva-99",
            fechaMillis = 1_719_191_919_000L,
            selecciones = mapOf(MenuIdentity.SECTION_DESSERT to "dish-dessert"),
            userId = "user-1"
        )

        val resolution = resolveConfirmacionReservaRequest(
            rawRequest = baseRawRequest(
                esEdicion = true,
                reservaId = reserva.id,
                fechaMillis = 123L,
                hasSeleccionesPendientesExtra = false,
                seleccionesPendientesRaw = emptyMap()
            ),
            reservaById = { reserva },
            canEditReservationDate = { millis -> millis == reserva.fechaMillis }
        )

        val request = (resolution as ConfirmacionReservaRequestResolution.Valid).request

        assertTrue(request.esEdicion)
        assertEquals(reserva, request.reserva)
        assertEquals(reserva.fechaMillis, request.fechaMillis)
        assertTrue(request.seleccionesPendientes.isEmpty())
    }

    @Test
    fun editRequestRejectsReservationWhenEditWindowBlocksItsDate() {
        val reserva = Reserva(
            id = "reserva-100",
            fechaMillis = 1_719_191_919_000L,
            selecciones = mapOf(MenuIdentity.SECTION_DESSERT to "dish-dessert"),
            userId = "user-1"
        )

        val resolution = resolveConfirmacionReservaRequest(
            rawRequest = baseRawRequest(
                esEdicion = true,
                reservaId = reserva.id,
                hasSeleccionesPendientesExtra = false,
                seleccionesPendientesRaw = emptyMap()
            ),
            reservaById = { reserva },
            canEditReservationDate = { false },
            canCreateReservationDate = { false }
        )

        assertEquals(
            ConfirmacionReservaRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible),
            resolution
        )
    }

    private fun baseRawRequest(
        esEdicion: Boolean = false,
        fecha: String = "Viernes 7/6/24",
        detalleSeleccion: String = "Plato principal: Milanesa",
        reservaId: String = "",
        fechaMillis: Long = 1_717_171_717_000L,
        hasSeleccionesPendientesExtra: Boolean = false,
        seleccionesPendientesRaw: Map<String, String> = emptyMap()
    ) = ConfirmacionReservaRawRequest(
        esEdicion = esEdicion,
        fecha = fecha,
        detalleSeleccion = detalleSeleccion,
        reservaId = reservaId,
        fechaMillis = fechaMillis,
        hasSeleccionesPendientesExtra = hasSeleccionesPendientesExtra,
        seleccionesPendientesRaw = seleccionesPendientesRaw
    )
}
