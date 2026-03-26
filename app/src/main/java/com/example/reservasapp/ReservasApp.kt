package com.example.reservasapp

import android.app.Application

class ReservasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppThemePreference.applySavedMode(this)
        SessionBootstrap.bootstrap()
    }
}
