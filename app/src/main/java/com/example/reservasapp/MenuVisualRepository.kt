package com.example.reservasapp

object MenuVisualRepository {

    fun buildItemsForSection(sectionName: String, options: List<String>): List<MenuItemOption> {
        return options.map { option ->
            MenuItemOption(
                name = option,
                description = descriptionFor(option, sectionName)
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

    fun imageForSelection(sectionName: String, selectedOption: String?): Int {
        val option = selectedOption?.takeIf { it.isNotBlank() } ?: return R.drawable.placeholder_comida
        return ComidaImageRepository.obtenerImagenComida(option)
    }
}
