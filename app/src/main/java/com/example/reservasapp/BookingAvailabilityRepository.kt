package com.example.reservasapp

import android.content.Context
import com.example.reservasapp.firebase.FirebaseProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar

data class BookingAvailabilityConfig(
    val enabledWeekdays: Set<Int>,
    val initialDelayDays: Int,
    val windowLengthDays: Int,
    val archiveRetentionDays: Int? = null
)

object BookingAvailabilityRepository {
    private const val COLLECTION_APP_CONFIG = "configuracion_app"
    private const val DOCUMENT_BOOKING_AVAILABILITY = "dias_habilitados_reservas"
    private const val FIELD_ENABLED_WEEKDAYS = "enabledWeekdays"
    private const val FIELD_INITIAL_DELAY_DAYS = "initialDelayDays"
    private const val FIELD_WINDOW_LENGTH_DAYS = "windowLengthDays"
    private const val FIELD_ARCHIVE_RETENTION_DAYS = "archiveRetentionDays"
    private const val FIELD_UPDATED_AT_MILLIS = "updatedAtMillis"

    private const val PREFS_NAME = "booking_availability_prefs"
    private const val PREF_ENABLED_WEEKDAYS = "enabled_weekdays"
    private const val PREF_INITIAL_DELAY_DAYS = "initial_delay_days"
    private const val PREF_WINDOW_LENGTH_DAYS = "window_length_days"
    private const val PREF_ARCHIVE_RETENTION_DAYS = "archive_retention_days"

    private val firestore by lazy { FirebaseProvider.firestore() }

    @Volatile
    private var cachedConfig: BookingAvailabilityConfig? = null

    fun obtenerConfiguracionActual(): BookingAvailabilityConfig {
        val current = cachedConfig
        if (current != null) {
            return current
        }

        val localConfig = readLocalConfigOrNull()
        if (localConfig != null) {
            cachedConfig = localConfig
            return localConfig
        }

        return defaultConfig()
    }

    fun cargarConfiguracion(onComplete: (Boolean, BookingAvailabilityConfig) -> Unit) {
        firestore.collection(COLLECTION_APP_CONFIG)
            .document(DOCUMENT_BOOKING_AVAILABILITY)
            .get()
            .addOnSuccessListener { doc ->
                val rawWeekdays = (doc.get(FIELD_ENABLED_WEEKDAYS) as? List<*>)
                    ?.mapNotNull { value ->
                        when (value) {
                            is Number -> value.toInt()
                            else -> null
                        }
                    }

                val config = BookingAvailabilityConfig(
                    enabledWeekdays = normalizePersistedWeekdays(rawWeekdays),
                    initialDelayDays = normalizePersistedInitialDelayDays(doc.getLong(FIELD_INITIAL_DELAY_DAYS)?.toInt()),
                    windowLengthDays = normalizePersistedWindowLengthDays(doc.getLong(FIELD_WINDOW_LENGTH_DAYS)?.toInt()),
                    archiveRetentionDays = normalizePersistedArchiveRetentionDays(doc.getLong(FIELD_ARCHIVE_RETENTION_DAYS)?.toInt())
                )
                updateLocalConfig(config)
                onComplete(true, config)
            }
            .addOnFailureListener {
                onComplete(false, obtenerConfiguracionActual())
            }
    }

    fun guardarConfiguracion(
        enabledWeekdays: Set<Int>,
        initialDelayDays: Int,
        windowLengthDays: Int,
        archiveRetentionDays: Int?,
        onComplete: (Boolean) -> Unit
    ) {
        val config = sanitizeConfig(
            BookingAvailabilityConfig(
                enabledWeekdays = enabledWeekdays,
                initialDelayDays = initialDelayDays,
                windowLengthDays = windowLengthDays,
                archiveRetentionDays = archiveRetentionDays
            )
        )
        if (config.enabledWeekdays.isEmpty()) {
            onComplete(false)
            return
        }

        val payload = mapOf(
            FIELD_ENABLED_WEEKDAYS to DAY_ORDER.filter { config.enabledWeekdays.contains(it) },
            FIELD_INITIAL_DELAY_DAYS to config.initialDelayDays,
            FIELD_WINDOW_LENGTH_DAYS to config.windowLengthDays,
            FIELD_ARCHIVE_RETENTION_DAYS to config.archiveRetentionDays,
            FIELD_UPDATED_AT_MILLIS to System.currentTimeMillis()
        )

        firestore.collection(COLLECTION_APP_CONFIG)
            .document(DOCUMENT_BOOKING_AVAILABILITY)
            .set(payload, SetOptions.merge())
            .addOnSuccessListener {
                updateLocalConfig(config)
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    fun estaFechaHabilitada(fechaMillis: Long): Boolean {
        return isWeekdayEnabled(fechaMillis, obtenerConfiguracionActual().enabledWeekdays)
    }

    fun obtenerFechaMaximaArchivable(referenceMillis: Long = System.currentTimeMillis()): Long? {
        return calculateArchiveRetentionLimitMillis(
            referenceMillis = referenceMillis,
            retentionDays = obtenerConfiguracionActual().archiveRetentionDays
        )
    }

    fun esRangoArchivable(hastaMillis: Long, referenceMillis: Long = System.currentTimeMillis()): Boolean {
        return isArchiveRangeAllowed(
            hastaMillis = hastaMillis,
            referenceMillis = referenceMillis,
            retentionDays = obtenerConfiguracionActual().archiveRetentionDays
        )
    }

    private fun updateLocalConfig(config: BookingAvailabilityConfig) {
        cachedConfig = config
        val prefs = ReservasApp.instanceOrNull()?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: return
        prefs.edit()
            .putString(
                PREF_ENABLED_WEEKDAYS,
                DAY_ORDER.filter { config.enabledWeekdays.contains(it) }.joinToString(",")
            )
            .putInt(PREF_INITIAL_DELAY_DAYS, config.initialDelayDays)
            .putInt(PREF_WINDOW_LENGTH_DAYS, config.windowLengthDays)
            .apply {
                if (config.archiveRetentionDays != null) {
                    putInt(PREF_ARCHIVE_RETENTION_DAYS, config.archiveRetentionDays)
                } else {
                    remove(PREF_ARCHIVE_RETENTION_DAYS)
                }
            }
            .apply()
    }

    private fun readLocalConfigOrNull(): BookingAvailabilityConfig? {
        val prefs = ReservasApp.instanceOrNull()?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?: return null
        val storedWeekdays = prefs.getString(PREF_ENABLED_WEEKDAYS, null)
            ?.split(',')
            ?.mapNotNull { value -> value.toIntOrNull() }

        return BookingAvailabilityConfig(
            enabledWeekdays = normalizePersistedWeekdays(storedWeekdays),
            initialDelayDays = normalizePersistedInitialDelayDays(
                prefs.takeIf { it.contains(PREF_INITIAL_DELAY_DAYS) }?.getInt(PREF_INITIAL_DELAY_DAYS, DEFAULT_INITIAL_DELAY_DAYS)
            ),
            windowLengthDays = normalizePersistedWindowLengthDays(
                prefs.takeIf { it.contains(PREF_WINDOW_LENGTH_DAYS) }?.getInt(PREF_WINDOW_LENGTH_DAYS, DEFAULT_WINDOW_LENGTH_DAYS)
            ),
            archiveRetentionDays = normalizePersistedArchiveRetentionDays(
                prefs.takeIf { it.contains(PREF_ARCHIVE_RETENTION_DAYS) }?.getInt(PREF_ARCHIVE_RETENTION_DAYS, 0)
            )
        )
    }

    internal fun clearCache() {
        cachedConfig = null
    }
}

internal const val DEFAULT_INITIAL_DELAY_DAYS = 0
internal const val DEFAULT_WINDOW_LENGTH_DAYS = 7
private const val MAX_BOOKING_WINDOW_DAYS = 365

internal val DAY_ORDER = listOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY,
    Calendar.SATURDAY,
    Calendar.SUNDAY
)

internal fun defaultEnabledWeekdays(): Set<Int> {
    return setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    )
}

internal fun sanitizeWeekdays(rawWeekdays: Collection<Int>): Set<Int> {
    return DAY_ORDER.filter { rawWeekdays.contains(it) }.toSet()
}

internal fun normalizePersistedWeekdays(rawWeekdays: Collection<Int>?): Set<Int> {
    val sanitizedWeekdays = sanitizeWeekdays(rawWeekdays.orEmpty())
    return if (sanitizedWeekdays.isEmpty()) {
        defaultEnabledWeekdays()
    } else {
        sanitizedWeekdays
    }
}

internal fun sanitizeInitialDelayDays(rawInitialDelayDays: Int): Int {
    return rawInitialDelayDays.coerceAtLeast(0).coerceAtMost(MAX_BOOKING_WINDOW_DAYS)
}

internal fun normalizePersistedInitialDelayDays(rawInitialDelayDays: Int?): Int {
    return sanitizeInitialDelayDays(rawInitialDelayDays ?: DEFAULT_INITIAL_DELAY_DAYS)
}

internal fun sanitizeWindowLengthDays(rawWindowLengthDays: Int): Int {
    return rawWindowLengthDays.coerceIn(1, MAX_BOOKING_WINDOW_DAYS)
}

internal fun normalizePersistedWindowLengthDays(rawWindowLengthDays: Int?): Int {
    return sanitizeWindowLengthDays(rawWindowLengthDays ?: DEFAULT_WINDOW_LENGTH_DAYS)
}

internal fun normalizePersistedArchiveRetentionDays(rawArchiveRetentionDays: Int?): Int? {
    return sanitizeArchiveRetentionDays(rawArchiveRetentionDays)
}

internal fun sanitizeArchiveRetentionDays(rawArchiveRetentionDays: Int?): Int? {
    return rawArchiveRetentionDays?.coerceIn(0, MAX_BOOKING_WINDOW_DAYS)
}

internal fun sanitizeConfig(rawConfig: BookingAvailabilityConfig): BookingAvailabilityConfig {
    return BookingAvailabilityConfig(
        enabledWeekdays = sanitizeWeekdays(rawConfig.enabledWeekdays),
        initialDelayDays = sanitizeInitialDelayDays(rawConfig.initialDelayDays),
        windowLengthDays = sanitizeWindowLengthDays(rawConfig.windowLengthDays),
        archiveRetentionDays = sanitizeArchiveRetentionDays(rawConfig.archiveRetentionDays)
    )
}

internal fun defaultConfig(): BookingAvailabilityConfig {
    return BookingAvailabilityConfig(
        enabledWeekdays = defaultEnabledWeekdays(),
        initialDelayDays = DEFAULT_INITIAL_DELAY_DAYS,
        windowLengthDays = DEFAULT_WINDOW_LENGTH_DAYS,
        archiveRetentionDays = null
    )
}

internal fun calculateArchiveRetentionLimitMillis(referenceMillis: Long, retentionDays: Int?): Long? {
    val sanitizedRetentionDays = sanitizeArchiveRetentionDays(retentionDays) ?: return null
    return Calendar.getInstance().apply {
        timeInMillis = referenceMillis
        clearTime()
        add(Calendar.DAY_OF_MONTH, -sanitizedRetentionDays)
    }.timeInMillis
}

internal fun isArchiveRangeAllowed(hastaMillis: Long, referenceMillis: Long, retentionDays: Int?): Boolean {
    val retentionLimitMillis = calculateArchiveRetentionLimitMillis(referenceMillis, retentionDays) ?: return true
    return hastaMillis <= retentionLimitMillis
}

internal fun isWeekdayEnabled(fechaMillis: Long, enabledWeekdays: Set<Int>): Boolean {
    val dayOfWeek = Calendar.getInstance().apply {
        timeInMillis = fechaMillis
    }.get(Calendar.DAY_OF_WEEK)

    return sanitizeWeekdays(enabledWeekdays).contains(dayOfWeek)
}

private fun Calendar.clearTime(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
