package com.example.reservasapp

object MenuVisualRepository {

    fun buildItemsForSection(sectionName: String, options: List<String>): List<MenuItemOption> {
        return options.map { option ->
            MenuItemOption(
                name = option,
                description = descriptionFor(option, sectionName),
                imageRes = imageForSelection(sectionName, option)
            )
        }
    }

    private fun descriptionFor(option: String, section: String): String {
        return when (option.lowercase()) {
            "pollo al horno" -> "400g de pollo al horno condimentado con especias"
            "milanesa napolitana" -> "Milanesa de carne con tomate y queso, condimentada con especias"
            "empanadas" -> "Empanadas surtidas de carne picante, pollo y jyq"
            "pure de papas" -> "Puré cremoso de papas, ideal para acompañar"
            "papas al horno" -> "Papas doradas al horno con toque de sal y hierbas"
            "ensalada mixta" -> "Lechuga, tomate y cebolla fresca"
            "flan" -> "Flan suave con textura cremosa"
            "gelatina" -> "Gelatina fresca de sabor frutal"
            "alfajor" -> "Alfajor clásico relleno de dulce"
            "fruta" -> "Fruta de estación seleccionada"
            else -> "Opción especial de $section para tu pedido"
        }
    }

    fun imageForSelection(section: String, option: String?): Int {
        if (option.isNullOrBlank()) return fallbackForSection(section)
        return imageFor(option, section)
    }

    private fun imageFor(option: String, section: String): Int {
        return when {
            option.equals("pollo al horno", true) -> R.drawable.pollo_al_horno
            option.contains("pollo", true) || option.contains("milanesa", true) || option.contains("empan", true) -> R.drawable.ic_food_main
            option.contains("pure", true) || option.contains("papas", true) || option.contains("ensalada", true) -> R.drawable.ic_side_dish
            option.contains("flan", true) || option.contains("gelatina", true) || option.contains("alfajor", true) || option.contains("fruta", true) -> R.drawable.ic_dessert
            else -> fallbackForSection(section)
        }
    }

    private fun fallbackForSection(section: String): Int {
        return when {
            section.contains("guarn", true) -> R.drawable.ic_side_dish
            section.contains("postre", true) -> R.drawable.ic_dessert
            else -> R.drawable.ic_food_main
        }
    }
}
