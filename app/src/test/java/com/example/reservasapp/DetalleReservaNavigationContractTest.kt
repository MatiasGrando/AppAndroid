package com.example.reservasapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetalleReservaNavigationContractTest {

    @Test
    fun createContractOnlyCarriesSelectedDate() {
        val contract = detalleReservaNavigationForCreate(dateMillis = 1_717_171_717_000L)

        assertEquals(1_717_171_717_000L, contract.selectedDateMillis)
        assertTrue(contract.reservaId.isBlank())
    }

    @Test
    fun editContractCarriesReservationIdAndDate() {
        val reserva = Reserva(
            id = "reserva-42",
            fechaMillis = 1_718_181_818_000L,
            selecciones = mapOf("Plato principal" to "Milanesa"),
            userId = "user-1"
        )

        val contract = detalleReservaNavigationForEdit(reserva)

        assertEquals("reserva-42", contract.reservaId)
        assertEquals(1_718_181_818_000L, contract.selectedDateMillis)
    }
}
