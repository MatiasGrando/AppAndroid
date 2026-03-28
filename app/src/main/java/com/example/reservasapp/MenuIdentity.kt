package com.example.reservasapp

object MenuIdentity {
    const val SECTION_MAIN = "main"
    const val SECTION_SIDE = "side"
    const val SECTION_DESSERT = "dessert"

    data class SectionDefinition(
        val id: String,
        val displayName: String
    )

    private val definitions = listOf(
        SectionDefinition(id = SECTION_MAIN, displayName = "Plato principal"),
        SectionDefinition(id = SECTION_SIDE, displayName = "Guarniciones"),
        SectionDefinition(id = SECTION_DESSERT, displayName = "Postres")
    )

    fun sectionDefinitions(): List<SectionDefinition> = definitions

    fun sectionDisplayName(sectionId: String): String {
        return definitions.firstOrNull { it.id == sectionId }?.displayName ?: sectionId
    }

    fun normalizeSectionId(rawSectionId: String?, rawSectionName: String?): String? {
        val normalizedId = rawSectionId.orEmpty().trim().lowercase()
        if (normalizedId.isNotBlank()) {
            return definitions.firstOrNull { it.id == normalizedId }?.id
        }

        val normalizedName = rawSectionName.orEmpty().trim().lowercase()
        return when (normalizedName) {
            "plato principal" -> SECTION_MAIN
            "guarniciones" -> SECTION_SIDE
            "postres" -> SECTION_DESSERT
            else -> null
        }
    }
}
