package com.example.reservasapp

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuTextNormalizerTest {

    @Test
    fun normalizeDishNameAplicaTitleCasePorPalabra() {
        assertEquals(
            "Churrasco de Carne",
            MenuTextNormalizer.normalizeDishName("churrasco de carne")
        )
    }

    @Test
    fun normalizeDishNameRecortaBordesYPreservaSeparadoresInternos() {
        assertEquals(
            "Milanesa   con   Pure",
            MenuTextNormalizer.normalizeDishName("  milanesa   con   pure  ")
        )
    }

    @Test
    fun normalizeDishNameCapitalizaConectorSiEsLaPrimeraPalabra() {
        assertEquals(
            "De la Huerta",
            MenuTextNormalizer.normalizeDishName("de la huerta")
        )
    }

    @Test
    fun normalizeDishDescriptionCapitalizaSoloElInicio() {
        assertEquals(
            "Con pure y salsa",
            MenuTextNormalizer.normalizeDishDescription("con pure y salsa")
        )
    }

    @Test
    fun normalizeDishDescriptionMantieneElRestoComoFueIngresado() {
        assertEquals(
            "Con Pure y SALSA",
            MenuTextNormalizer.normalizeDishDescription("  con Pure y SALSA  ")
        )
    }
}
