package com.example.reservasapp.branding

import com.example.reservasapp.firebase.FirebaseConfig

interface ClientProfile {
    val id: String
    val buyerName: String
    val branding: BrandingConfig
    val featureFlags: FeatureFlags
    val firebase: FirebaseConfig
}
