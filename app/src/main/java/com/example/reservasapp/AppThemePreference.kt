package com.example.reservasapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppThemePreference {

    private const val APP_THEME_PREFS = "app_theme_prefs"
    private const val KEY_NIGHT_MODE = "night_mode"
    private const val DEFAULT_NIGHT_MODE = AppCompatDelegate.MODE_NIGHT_YES

    fun applySavedMode(context: Context) {
        clearLegacyPreference(context)
        AppCompatDelegate.setDefaultNightMode(DEFAULT_NIGHT_MODE)
    }

    private fun clearLegacyPreference(context: Context) {
        val prefs = context.getSharedPreferences(APP_THEME_PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_NIGHT_MODE)) {
            prefs.edit().remove(KEY_NIGHT_MODE).apply()
        }
    }
}

