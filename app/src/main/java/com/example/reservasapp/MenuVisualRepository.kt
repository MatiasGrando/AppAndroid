package com.example.reservasapp

object MenuVisualRepository {

    fun buildItemsForSection(sectionName: String, options: List<String>): List<MenuItemOption> {
        return options.mapIndexed { index, option ->
            MenuItemOption(
                name = option,
                description = descriptionFor(option, sectionName),
                price = priceFor(index),
                imageRes = imageFor(option, sectionName)
            )
        }
    }

    private fun descriptionFor(option: String, section: String): String {
        val base = when {
            option.contains("pollo", true) -> "Jugoso pollo al horno con especias suaves"
            option.contains("carne", true) -> "Corte tierno cocido al punto"
            option.contains("helado", true) -> "Postre frío y cremoso"
            option.contains("alfajor", true) -> "Dulce relleno con cobertura suave"
            else -> "Opción especial de $section para tu pedido"
        }
        return base
    }

    private fun priceFor(index: Int): String {
        val base = 8.50 + (index * 1.25)
        return "$${"%.2f".format(base)}"
    }

    private fun imageFor(option: String, section: String): Int {
        return when {
            option.contains("pollo", true) -> android.R.drawable.ic_menu_camera
            option.contains("carne", true) -> android.R.drawable.ic_menu_compass
            option.contains("helado", true) -> android.R.drawable.ic_menu_gallery
            option.contains("alfajor", true) -> android.R.drawable.ic_menu_crop
            section.contains("guarn", true) -> android.R.drawable.ic_menu_agenda
            else -> android.R.drawable.ic_menu_report_image
        }
    }
}
