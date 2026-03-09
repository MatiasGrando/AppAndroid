package com.example.reservasapp

data class Reserva(
    val id: String,
    val fechaMillis: Long,
    val selecciones: Map<String, String>,
    val userId: String
)
