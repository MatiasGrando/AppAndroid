package com.example.reservasapp.branding

import android.content.Context
import android.content.pm.PackageManager

object ClientProfileResolver {
    const val manifestKey: String = "com.example.reservasapp.CLIENT_PROFILE"

    private val profilesById: Map<String, ClientProfile> = listOf(
        LunchPointProfile,
        SuperViandasProfile
    ).associateBy(ClientProfile::id)

    fun resolve(context: Context): ClientProfile {
        val requestedProfileId = resolveConfiguredProfileId(context)
        return profilesById[requestedProfileId] ?: LunchPointProfile
    }

    /**
     * Alternar buyer = cambiar el valor del meta-data
     * `com.example.reservasapp.CLIENT_PROFILE` en AndroidManifest.xml.
     * Valores soportados hoy: `lunch-point`, `super-viandas`.
     */
    fun availableProfiles(): Set<String> {
        return profilesById.keys
    }

    private fun resolveConfiguredProfileId(context: Context): String? {
        return runCatching {
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString(manifestKey)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }.getOrNull()
    }
}
