package com.example.reservasapp

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

private const val APP_THEME_PREFS = "app_theme_prefs"
private const val KEY_NIGHT_MODE = "night_mode"

object AppThemePreference {

    fun applySavedMode(context: Context) {
        AppCompatDelegate.setDefaultNightMode(getNightMode(context))
    }

    fun toggle(context: Context): Int {
        val updatedMode = if (isDarkModeEnabled(context)) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        saveNightMode(context, updatedMode)
        AppCompatDelegate.setDefaultNightMode(updatedMode)
        return updatedMode
    }

    fun isDarkModeEnabled(context: Context): Boolean {
        return getNightMode(context) == AppCompatDelegate.MODE_NIGHT_YES
    }

    private fun getNightMode(context: Context): Int {
        val prefs = context.getSharedPreferences(APP_THEME_PREFS, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES)
    }

    private fun saveNightMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(APP_THEME_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply()
    }
}

