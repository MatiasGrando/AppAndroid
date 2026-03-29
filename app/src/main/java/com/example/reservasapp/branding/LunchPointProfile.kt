package com.example.reservasapp.branding

import com.example.reservasapp.R
import com.example.reservasapp.firebase.FirebaseConfig

object LunchPointProfile : ClientProfile {
    override val id: String = "lunch-point"
    override val buyerName: String = "Lunch Point"
    override val branding: BrandingConfig = BrandingConfig(
        appNameRes = R.string.branding_lunch_point_app_name,
        loginTitleRes = R.string.branding_lunch_point_login_title,
        homeTitleRes = R.string.branding_lunch_point_home_title,
        homePrimaryActionRes = R.string.branding_lunch_point_home_primary_action,
        homeSecondaryActionRes = R.string.branding_lunch_point_home_secondary_action,
        appLogoRes = R.mipmap.ic_launcher,
        loginDecorRes = R.drawable.landing_background,
        homeBackgroundRes = R.drawable.bg_main_soft,
        confirmationBackgroundRes = R.drawable.bg_asian_food,
        loginBackgroundColorRes = R.color.branding_lunch_point_login_background,
        loginOverlayColorRes = R.color.branding_lunch_point_login_overlay,
        homeOverlayColorRes = R.color.branding_lunch_point_home_overlay,
        primaryActionColorRes = R.color.branding_lunch_point_primary_action,
        secondaryActionColorRes = R.color.branding_lunch_point_secondary_action,
        actionTextColorRes = R.color.branding_lunch_point_action_text,
        confirmationTitleColorRes = R.color.branding_lunch_point_confirmation_title,
        confirmationBodyTextColorRes = R.color.branding_lunch_point_confirmation_body,
        confirmationCardBackgroundColorRes = R.color.branding_lunch_point_confirmation_card_background,
        confirmationCardStrokeColorRes = R.color.branding_lunch_point_confirmation_card_stroke
    )
    override val featureFlags: FeatureFlags = FeatureFlags(
        brandedConfirmationScreen = true
    )
    override val firebase: FirebaseConfig = FirebaseConfig(
        applicationId = "1:481195326232:android:b293cfb632b28e03ed7c6d",
        apiKey = "AIzaSyA6cG9UCeo4BCBiYtHN1WgOpdPPkK9GydI",
        projectId = "lunch-point",
        storageBucket = "lunch-point.firebasestorage.app",
        googleWebClientId = "481195326232-g2ks22mvnf7nml2d5ues08u24ffv8r1s.apps.googleusercontent.com"
    )
}
