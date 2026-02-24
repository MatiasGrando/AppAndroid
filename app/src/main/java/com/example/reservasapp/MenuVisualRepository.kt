package com.example.reservasapp

object MenuVisualRepository {

    fun buildItemsForSection(sectionName: String, options: List<String>): List<MenuItemOption> {
        return options.map { option ->
            MenuItemOption(
                name = option,
                description = descriptionFor(option, sectionName),
                imageRes = imageFor(option, sectionName)
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

    private fun imageFor(option: String, section: String): Int {
        return when {
            option.contains("pollo", true) -> android.R.drawable.ic_menu_camera
            option.contains("milanesa", true) || option.contains("carne", true) -> android.R.drawable.ic_menu_compass
            option.contains("empan", true) -> android.R.drawable.ic_menu_gallery
            option.contains("pure", true) || option.contains("papas", true) -> android.R.drawable.ic_menu_agenda
            option.contains("ensalada", true) || option.contains("fruta", true) -> android.R.drawable.ic_menu_myplaces
            option.contains("flan", true) || option.contains("gelatina", true) || option.contains("alfajor", true) -> android.R.drawable.ic_menu_crop
            section.contains("postre", true) -> android.R.drawable.ic_menu_crop
            else -> android.R.drawable.ic_menu_report_image
        }
    }
}
