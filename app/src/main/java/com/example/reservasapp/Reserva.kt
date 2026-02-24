package com.example.reservasapp

data class Reserva(
    val id: Long,
    val fechaMillis: Long,
    val selecciones: Map<String, String>
)
