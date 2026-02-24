package com.example.reservasapp

data class Reserva(
    val id: Long,
    val fechaMillis: Long,
    val comida: String,
    val postre: String
)
