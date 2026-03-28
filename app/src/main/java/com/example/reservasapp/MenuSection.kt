package com.example.reservasapp

data class MenuSection(
    val id: String = "",
    val nombre: String,
    val opciones: MutableList<MenuDish>
)
