package com.example.reservasapp

import java.util.Calendar
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookingAvailabilityRepositoryTest {

    @Before
    fun setUp() {
        BookingAvailabilityRepository.clearCache()
    }

    @After
    fun tearDown() {
        BookingAvailabilityRepository.clearCache()
    }

    @Test
    fun normalizePersistedWeekdaysFallsBackToWeekdaysWhenConfigIsMissing() {
        assertEquals(defaultEnabledWeekdays(), normalizePersistedWeekdays(null))
        assertEquals(defaultEnabledWeekdays(), normalizePersistedWeekdays(emptySet()))
        assertEquals(defaultEnabledWeekdays(), normalizePersistedWeekdays(setOf(0, -1, 99)))
    }

    @Test
    fun normalizePersistedWindowConfigFallsBackToSafeDefaults() {
        assertEquals(DEFAULT_INITIAL_DELAY_DAYS, normalizePersistedInitialDelayDays(null))
        assertEquals(DEFAULT_INITIAL_DELAY_DAYS, normalizePersistedInitialDelayDays(-5))
        assertEquals(DEFAULT_WINDOW_LENGTH_DAYS, normalizePersistedWindowLengthDays(null))
        assertEquals(1, normalizePersistedWindowLengthDays(0))
        assertNull(normalizePersistedArchiveRetentionDays(null))
        assertEquals(0, normalizePersistedArchiveRetentionDays(-5))
    }

    @Test
    fun sanitizeWeekdaysKeepsOnlySupportedCalendarConstants() {
        val sanitized = sanitizeWeekdays(setOf(Calendar.MONDAY, Calendar.SATURDAY, 99, -1))

        assertEquals(setOf(Calendar.MONDAY, Calendar.SATURDAY), sanitized)
    }

    @Test
    fun isWeekdayEnabledRespectsWeekendSelection() {
        val nextSaturday = nextDateFor(Calendar.SATURDAY)

        assertFalse(isWeekdayEnabled(nextSaturday, defaultEnabledWeekdays()))
        assertTrue(isWeekdayEnabled(nextSaturday, DAY_ORDER.toSet()))
    }

    @Test
    fun sanitizeConfigNormalizesWeekdaysAndWindowValues() {
        val config = sanitizeConfig(
            BookingAvailabilityConfig(
                enabledWeekdays = setOf(Calendar.MONDAY, 99),
                initialDelayDays = -2,
                windowLengthDays = 0,
                archiveRetentionDays = -30
            )
        )

        assertEquals(setOf(Calendar.MONDAY), config.enabledWeekdays)
        assertEquals(0, config.initialDelayDays)
        assertEquals(1, config.windowLengthDays)
        assertEquals(0, config.archiveRetentionDays)
    }

    @Test
    fun archiveRetentionHelpersCalculateAndValidateLimit() {
        val referenceMillis = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 28, 15, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val limitMillis = calculateArchiveRetentionLimitMillis(referenceMillis, 7)
        val expectedLimitMillis = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 21, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(expectedLimitMillis, limitMillis)
        assertTrue(isArchiveRangeAllowed(expectedLimitMillis, referenceMillis, 7))
        assertFalse(isArchiveRangeAllowed(expectedLimitMillis + 1, referenceMillis, 7))
        assertTrue(isArchiveRangeAllowed(referenceMillis, referenceMillis, null))
    }

    @Test
    fun obtenerConfiguracionActualUsaDefaultsSiLaAplicacionNoEstaInicializada() {
        val config = BookingAvailabilityRepository.obtenerConfiguracionActual()

        assertEquals(defaultConfig(), config)
    }

    private fun nextDateFor(dayOfWeek: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
    }
}
