package com.example.reservasapp

import java.util.Locale

internal object MenuTextNormalizer {
    fun normalizeDishName(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return trimmed

        var wordIndex = 0
        return WORD_REGEX.replace(trimmed) { match ->
            val normalizedWord = normalizeDishNameWord(match.value, wordIndex == 0)
            wordIndex += 1
            normalizedWord
        }
    }

    fun normalizeDishDescription(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return trimmed

        return trimmed.replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.ROOT)
            } else {
                char.toString()
            }
        }
    }

    private val WORD_REGEX = Regex("\\S+")

    private val LOWERCASE_WORDS = setOf(
        "a",
        "al",
        "con",
        "de",
        "del",
        "e",
        "el",
        "en",
        "la",
        "las",
        "los",
        "o",
        "sin",
        "u",
        "un",
        "una",
        "unos",
        "unas",
        "y"
    )

    private fun normalizeDishNameWord(value: String, isFirstWord: Boolean): String {
        val lowercased = value.lowercase(Locale.ROOT)
        if (!isFirstWord && lowercased in LOWERCASE_WORDS) {
            return lowercased
        }

        return lowercased.replaceFirstChar { char ->
            if (char.isLowerCase()) {
                char.titlecase(Locale.ROOT)
            } else {
                char.toString()
            }
        }
    }
}
