package com.example.reservasapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuRepositoryBackfillTest {

    @Test
    fun legacyDishIdsByKeyUsaLaClaveLegacyEsperada() {
        val dishIdsByKey = MenuRepository.legacyDishIdsByKey(menuBase())

        assertEquals("dish-main-1", dishIdsByKey["plato principal|milanesa napolitana"])
        assertEquals("dish-side-1", dishIdsByKey["guarniciones|pure de papas"])
    }

    @Test
    fun planLegacyMenuByDateBackfillReemplazaIdsCuandoPuedeResolverTodo() {
        val payload = MenuRepository.planLegacyMenuByDateBackfill(
            documentId = "2026-03-28",
            documentData = mapOf(
                "enabledDishKeys" to listOf("plato principal|milanesa napolitana", "guarniciones|pure de papas"),
                "enabledDishIds" to listOf("legacy-id")
            ),
            dishIdsByLegacyKey = MenuRepository.legacyDishIdsByKey(menuBase())
        )

        assertEquals(
            mapOf(
                "enabledDishIds" to listOf("dish-main-1", "dish-side-1"),
                "dateKey" to "2026-03-28"
            ),
            payload
        )
    }

    @Test
    fun planLegacyMenuByDateBackfillConservaIdsExistentesSiLaResolucionEsParcial() {
        val payload = MenuRepository.planLegacyMenuByDateBackfill(
            documentId = "2026-03-28",
            documentData = mapOf(
                "dateKey" to "2026-03-28",
                "enabledDishKeys" to listOf("plato principal|milanesa napolitana", "postres|ya no existe"),
                "enabledDishIds" to listOf("dish-dessert-legacy")
            ),
            dishIdsByLegacyKey = MenuRepository.legacyDishIdsByKey(menuBase())
        )

        assertEquals(
            mapOf("enabledDishIds" to listOf("dish-dessert-legacy", "dish-main-1")),
            payload
        )
    }

    @Test
    fun planLegacyMenuByDateBackfillNoEscribeNadaSiNoPuedeResolverNiTieneIdsPrevios() {
        val payload = MenuRepository.planLegacyMenuByDateBackfill(
            documentId = "2026-03-28",
            documentData = mapOf(
                "enabledDishKeys" to listOf("postres|ya no existe")
            ),
            dishIdsByLegacyKey = MenuRepository.legacyDishIdsByKey(menuBase())
        )

        assertNull(payload)
    }

    private fun menuBase(): List<MenuSection> {
        return listOf(
            MenuSection(
                id = MenuIdentity.SECTION_MAIN,
                nombre = "Plato principal",
                opciones = mutableListOf(
                    MenuDish(
                        id = "dish-main-1",
                        nombre = "Milanesa napolitana",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = true
                    )
                )
            ),
            MenuSection(
                id = MenuIdentity.SECTION_SIDE,
                nombre = "Guarniciones",
                opciones = mutableListOf(
                    MenuDish(
                        id = "dish-side-1",
                        nombre = "Pure de papas",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            ),
            MenuSection(
                id = MenuIdentity.SECTION_DESSERT,
                nombre = "Postres",
                opciones = mutableListOf(
                    MenuDish(
                        id = "dish-dessert-1",
                        nombre = "Flan casero",
                        detalle = "",
                        imageUrl = "",
                        guarnicion = false
                    )
                )
            )
        )
    }
}
