package com.example.reservasapp

object MenuVisualRepository {

    fun buildItemsForSection(options: List<MenuDish>): List<MenuItemOption> {
        return options.map { option ->
            MenuItemOption(
                id = option.id,
                name = option.nombre,
                description = option.detalle,
                imageUrl = option.imageUrl
            )
        }
    }
}
