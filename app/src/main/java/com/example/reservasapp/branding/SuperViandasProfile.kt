package com.example.reservasapp.branding

import com.example.reservasapp.R
import com.example.reservasapp.firebase.FirebaseConfig

object SuperViandasProfile : ClientProfile {
    override val id: String = "super-viandas"
    override val buyerName: String = "Super Viandas"
    override val branding: BrandingConfig = BrandingConfig(
        appNameRes = R.string.branding_super_viandas_app_name,
        loginTitleRes = R.string.branding_super_viandas_login_title,
        homeTitleRes = R.string.branding_super_viandas_home_title,
        homeSubtitleRes = 0,
        homePrimaryActionRes = R.string.branding_super_viandas_home_primary_action,
        homeSecondaryActionRes = R.string.branding_super_viandas_home_secondary_action,
        appLogoRes = R.mipmap.ic_launcher,
        homeHeroLogoRes = 0,
        loginDecorRes = R.drawable.landing_background,
        homeBackgroundRes = R.drawable.bg_main_soft,
        confirmationBackgroundRes = R.drawable.bg_asian_food,
        loginBackgroundColorRes = R.color.branding_super_viandas_login_background,
        loginOverlayColorRes = R.color.branding_super_viandas_login_overlay,
        homeOverlayColorRes = R.color.branding_super_viandas_home_overlay,
        primaryActionColorRes = R.color.branding_super_viandas_primary_action,
        secondaryActionColorRes = R.color.branding_super_viandas_secondary_action,
        actionTextColorRes = R.color.branding_super_viandas_action_text,
        confirmationTitleColorRes = R.color.branding_super_viandas_confirmation_title,
        confirmationBodyTextColorRes = R.color.branding_super_viandas_confirmation_body,
        confirmationCardBackgroundColorRes = R.color.branding_super_viandas_confirmation_card_background,
        confirmationCardStrokeColorRes = R.color.branding_super_viandas_confirmation_card_stroke
    )
    override val featureFlags: FeatureFlags = FeatureFlags(
        brandedConfirmationScreen = true
    )
    override val firebase: FirebaseConfig = FirebaseConfig(
        applicationId = "1:000000000000:android:superviandasdummy0001",
        apiKey = "AIzaSyDummySuperViandasKey000000000000000",
        projectId = "super-viandas-dummy",
        storageBucket = "super-viandas-dummy.firebasestorage.app",
        googleWebClientId = "000000000000-superviandasdummy.apps.googleusercontent.com"
    )
}
