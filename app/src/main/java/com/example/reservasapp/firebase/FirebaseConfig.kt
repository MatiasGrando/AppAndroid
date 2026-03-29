package com.example.reservasapp.firebase

import com.google.firebase.FirebaseOptions

data class FirebaseConfig(
    val applicationId: String,
    val apiKey: String,
    val projectId: String,
    val storageBucket: String,
    val googleWebClientId: String
) {
    fun toFirebaseOptions(): FirebaseOptions {
        return FirebaseOptions.Builder()
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .setProjectId(projectId)
            .setStorageBucket(storageBucket)
            .build()
    }
}
