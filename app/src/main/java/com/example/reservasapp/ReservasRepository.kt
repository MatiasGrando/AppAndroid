package com.example.reservasapp

import java.util.Calendar

object ReservasRepository {
    private val reservas = mutableListOf<Reserva>()

    fun agregarReserva(reserva: Reserva) {
        reservas.add(reserva)
    }

    fun obtenerReservasProximosSieteDias(): List<Reserva> {
        val inicioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val finRango = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.DAY_OF_YEAR, 6)
        }.timeInMillis

        return reservas
            .filter { it.fechaMillis in inicioHoy..finRango }
            .sortedBy { it.fechaMillis }
    }
}
