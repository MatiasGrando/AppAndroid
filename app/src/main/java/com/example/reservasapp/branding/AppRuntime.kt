package com.example.reservasapp.branding

import android.content.Context
import com.example.reservasapp.firebase.FirebaseConfig

object AppRuntime {
    private lateinit var activeProfile: ClientProfile

    fun initialize(context: Context) {
        activeProfile = ClientProfileResolver.resolve(context.applicationContext)
    }

    val profile: ClientProfile
        get() = activeProfile

    val isInitialized: Boolean
        get() = ::activeProfile.isInitialized

    val branding: BrandingConfig
        get() = profile.branding

    val featureFlags: FeatureFlags
        get() = profile.featureFlags

    val firebase: FirebaseConfig
        get() = profile.firebase
}
