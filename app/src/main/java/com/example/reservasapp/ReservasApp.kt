package com.example.reservasapp

import android.app.Application
import com.example.reservasapp.branding.AppRuntime
import com.example.reservasapp.firebase.FirebaseProvider

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
        AppRuntime.initialize(this)
        FirebaseProvider.initialize(this)
        AppThemePreference.applySavedMode(this)
    }
}
