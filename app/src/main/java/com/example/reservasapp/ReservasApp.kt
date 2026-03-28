package com.example.reservasapp

import android.app.Application

class ReservasApp : Application() {
    companion object {
        lateinit var instance: ReservasApp
            private set

        fun instanceOrNull(): ReservasApp? {
            return if (::instance.isInitialized) instance else null
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppThemePreference.applySavedMode(this)
    }
}
