package com.example.reservasapp.firebase

import android.content.Context
import com.example.reservasapp.branding.AppRuntime
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseProvider {
    fun initialize(context: Context) {
        if (!AppRuntime.isInitialized) {
            return
        }

        val appName = runtimeAppName()
        val appContext = context.applicationContext
        val existingApp = FirebaseApp.getApps(appContext).firstOrNull { it.name == appName }
        if (existingApp != null) {
            return
        }

        FirebaseApp.initializeApp(appContext, AppRuntime.profile.firebase.toFirebaseOptions(), appName)
    }

    fun auth(): FirebaseAuth {
        return resolveApp()?.let { FirebaseAuth.getInstance(it) } ?: FirebaseAuth.getInstance()
    }

    fun firestore(): FirebaseFirestore {
        return resolveApp()?.let { FirebaseFirestore.getInstance(it) } ?: FirebaseFirestore.getInstance()
    }

    fun storage(): FirebaseStorage {
        return resolveApp()?.let { FirebaseStorage.getInstance(it) } ?: FirebaseStorage.getInstance()
    }

    fun googleWebClientId(): String {
        return AppRuntime.profile.firebase.googleWebClientId
    }

    private fun resolveApp(): FirebaseApp? {
        if (!AppRuntime.isInitialized) {
            return null
        }

        return runCatching { FirebaseApp.getInstance(runtimeAppName()) }.getOrNull()
    }

    private fun runtimeAppName(): String {
        return "buyer-${AppRuntime.profile.id}"
    }
}
