package com.example.reservasapp

data class Usuario(
    val userId: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val dni: String = "",
    val email: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
)

data class Admin(
    val userId: String = "",
    val email: String = "",
    val rol: String = "admin"
)

data class Pedido(
    val idPedido: String = "",
    val userId: String = "",
    val nombreUsuario: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val items: List<String> = emptyList(),
    val total: Double = 0.0,
    val estadoPedido: String = ESTADO_PENDIENTE
) {
    companion object {
        const val ESTADO_PENDIENTE = "pendiente"
        const val ESTADO_CONFIRMADO = "confirmado"
        const val ESTADO_ENTREGADO = "entregado"

        val estadosValidos = listOf(ESTADO_PENDIENTE, ESTADO_CONFIRMADO, ESTADO_ENTREGADO)
    }
}
