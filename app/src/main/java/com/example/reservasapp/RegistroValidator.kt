package com.example.reservasapp

object RegistroValidator {
    private val dniRegex = Regex("^[0-9]{7,8}$")

    fun esNombreValido(valor: String): Boolean = valor.trim().isNotEmpty()

    fun esDniValido(valor: String): Boolean = dniRegex.matches(valor.trim())
}
