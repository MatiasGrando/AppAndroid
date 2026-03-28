package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class AdminActivity : BaseActivity() {
    private lateinit var weekdayCheckboxes: List<Pair<Int, CheckBox>>
    private lateinit var bookingDaysSummary: TextView
    private lateinit var bookingDaysHint: TextView
    private lateinit var bookingWindowSummary: TextView
    private lateinit var initialDelayInput: EditText
    private lateinit var windowLengthInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAdminAccess()) {
            return
        }

        setContentView(R.layout.activity_admin)

        bookingDaysSummary = findViewById(R.id.tvBookingDaysSummary)
        bookingDaysHint = findViewById(R.id.tvBookingDaysHint)
        bookingWindowSummary = findViewById(R.id.tvBookingWindowSummary)
        initialDelayInput = findViewById(R.id.etBookingInitialDelay)
        windowLengthInput = findViewById(R.id.etBookingWindowLength)
        weekdayCheckboxes = listOf(
            Calendar.MONDAY to findViewById(R.id.checkMonday),
            Calendar.TUESDAY to findViewById(R.id.checkTuesday),
            Calendar.WEDNESDAY to findViewById(R.id.checkWednesday),
            Calendar.THURSDAY to findViewById(R.id.checkThursday),
            Calendar.FRIDAY to findViewById(R.id.checkFriday),
            Calendar.SATURDAY to findViewById(R.id.checkSaturday),
            Calendar.SUNDAY to findViewById(R.id.checkSunday)
        )

        findViewById<Button>(R.id.btnConfigMenuFechaAdmin).setOnClickListener {
            startActivity(Intent(this, AdminMenuFechaActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddMenuAdmin).setOnClickListener {
            startActivity(Intent(this, AdminMenuActivity::class.java))
        }

        findViewById<Button>(R.id.btnResumenPedidosDiaAdmin).setOnClickListener {
            startActivity(Intent(this, PedidosPorDiaActivity::class.java))
        }

        findViewById<Button>(R.id.btnDetallePedidosUsuariosAdmin).setOnClickListener {
            startActivity(Intent(this, DetallePedidosUsuariosActivity::class.java))
        }

        findViewById<Button>(R.id.btnPresetWeekdays).setOnClickListener {
            applyWeekdaySelection(defaultEnabledWeekdays())
        }

        findViewById<Button>(R.id.btnPresetFullWeek).setOnClickListener {
            applyWeekdaySelection(DAY_ORDER.toSet())
        }

        findViewById<Button>(R.id.btnGuardarBookingDaysAdmin).setOnClickListener {
            guardarDiasHabilitados()
        }

        loadBookingAvailabilityConfig()
    }

    private fun loadBookingAvailabilityConfig() {
        setBookingConfigControlsEnabled(false)
        bookingDaysHint.text = getString(R.string.admin_booking_days_loading)

        BookingAvailabilityRepository.cargarConfiguracion { ok, config ->
            runOnUiThread {
                applyWeekdaySelection(config.enabledWeekdays)
                applyBookingWindow(config)
                bookingDaysHint.text = getString(R.string.admin_booking_days_hint)
                setBookingConfigControlsEnabled(true)

                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_booking_days, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarDiasHabilitados() {
        val enabledWeekdays = weekdayCheckboxes
            .filter { (_, checkBox) -> checkBox.isChecked }
            .map { (dayOfWeek, _) -> dayOfWeek }
            .toSet()
        val initialDelayDays = initialDelayInput.text.toString().toIntOrNull()
        val windowLengthDays = windowLengthInput.text.toString().toIntOrNull()

        if (enabledWeekdays.isEmpty()) {
            Toast.makeText(this, R.string.error_guardar_booking_days_vacio, Toast.LENGTH_SHORT).show()
            return
        }

        if (initialDelayDays == null || initialDelayDays < 0) {
            Toast.makeText(this, R.string.error_guardar_booking_initial_delay, Toast.LENGTH_SHORT).show()
            return
        }

        if (windowLengthDays == null || windowLengthDays <= 0) {
            Toast.makeText(this, R.string.error_guardar_booking_window_length, Toast.LENGTH_SHORT).show()
            return
        }

        val sanitizedInitialDelayDays = sanitizeInitialDelayDays(initialDelayDays)
        val sanitizedWindowLengthDays = sanitizeWindowLengthDays(windowLengthDays)

        setBookingConfigControlsEnabled(false)
        BookingAvailabilityRepository.guardarConfiguracion(
            enabledWeekdays = enabledWeekdays,
            initialDelayDays = sanitizedInitialDelayDays,
            windowLengthDays = sanitizedWindowLengthDays
        ) { ok ->
            runOnUiThread {
                setBookingConfigControlsEnabled(true)
                if (!ok) {
                    Toast.makeText(this, R.string.error_guardar_booking_days, Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                updateBookingDaysSummary(enabledWeekdays)
                initialDelayInput.setText(sanitizedInitialDelayDays.toString())
                windowLengthInput.setText(sanitizedWindowLengthDays.toString())
                updateBookingWindowSummary(sanitizedInitialDelayDays, sanitizedWindowLengthDays)
                Toast.makeText(this, R.string.booking_availability_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun applyWeekdaySelection(enabledWeekdays: Set<Int>) {
        weekdayCheckboxes.forEach { (dayOfWeek, checkBox) ->
            checkBox.isChecked = enabledWeekdays.contains(dayOfWeek)
        }
        updateBookingDaysSummary(enabledWeekdays)
    }

    private fun applyBookingWindow(config: BookingAvailabilityConfig) {
        initialDelayInput.setText(config.initialDelayDays.toString())
        windowLengthInput.setText(config.windowLengthDays.toString())
        updateBookingWindowSummary(config.initialDelayDays, config.windowLengthDays)
    }

    private fun updateBookingDaysSummary(enabledWeekdays: Set<Int>) {
        val summaryRes = when {
            enabledWeekdays == defaultEnabledWeekdays() -> R.string.admin_booking_days_summary_weekdays
            enabledWeekdays == DAY_ORDER.toSet() -> R.string.admin_booking_days_summary_full_week
            else -> R.string.admin_booking_days_summary_custom
        }
        bookingDaysSummary.text = getString(summaryRes)
    }

    private fun updateBookingWindowSummary(initialDelayDays: Int, windowLengthDays: Int) {
        bookingWindowSummary.text = getString(
            R.string.admin_booking_window_summary,
            initialDelayDays,
            windowLengthDays
        )
    }

    private fun setBookingConfigControlsEnabled(enabled: Boolean) {
        findViewById<Button>(R.id.btnPresetWeekdays).isEnabled = enabled
        findViewById<Button>(R.id.btnPresetFullWeek).isEnabled = enabled
        findViewById<Button>(R.id.btnGuardarBookingDaysAdmin).isEnabled = enabled
        initialDelayInput.isEnabled = enabled
        windowLengthInput.isEnabled = enabled
        weekdayCheckboxes.forEach { (_, checkBox) ->
            checkBox.isEnabled = enabled
        }
    }
}
