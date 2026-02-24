package com.example.reservasapp

import java.util.Calendar

object ReservasRepository {
    private val reservas = mutableListOf<Reserva>()
    private var nextId = 1L

    fun agregarReserva(fechaMillis: Long, selecciones: Map<String, String>): Reserva {
        val reserva = Reserva(
            id = nextId++,
            fechaMillis = fechaMillis,
            selecciones = selecciones.toMap()
        )
        reservas.add(reserva)
        return reserva
    }

    fun actualizarReserva(id: Long, selecciones: Map<String, String>): Reserva? {
        val index = reservas.indexOfFirst { it.id == id }
        if (index == -1) return null

        val actual = reservas[index]
        val actualizada = actual.copy(selecciones = selecciones.toMap())
        reservas[index] = actualizada
        return actualizada
    }

    fun obtenerReservaPorId(id: Long): Reserva? = reservas.firstOrNull { it.id == id }

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

    fun formatearSelecciones(selecciones: Map<String, String>): String {
        return selecciones.entries.joinToString(" | ") { "${it.key}: ${it.value}" }
    }
}
