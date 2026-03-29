package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar

class ReservarActivity : BaseActivity() {
    private lateinit var monthLabel: TextView
    private lateinit var gridWeekdays: GridLayout
    private lateinit var gridDays: GridLayout
    private lateinit var continueButton: Button
    private lateinit var flowStatusText: TextView

    private var today: Calendar = Calendar.getInstance().clearTime()
    private var minReservableDate: Calendar = Calendar.getInstance().clearTime()
    private var maxReservableDate: Calendar = Calendar.getInstance().clearTime().apply {
        add(Calendar.DAY_OF_YEAR, 6)
    }

    private var visibleMonth: Calendar = Calendar.getInstance().clearTime().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    private var selectedDateMillis: Long = today.timeInMillis
    private var hasUserSelectedDate: Boolean = false
    private var reservedDates: Set<Long> = emptySet()
    private var lastTappedDateMillis: Long = -1L
    private var lastDateTapTimestamp: Long = 0L
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", spanishDateLocale)
    private val weekdayLabels by lazy {
        DateFormatSymbols(spanishDateLocale).shortWeekdays.let { weekdays ->
            listOf(
                weekdays[Calendar.MONDAY],
                weekdays[Calendar.TUESDAY],
                weekdays[Calendar.WEDNESDAY],
                weekdays[Calendar.THURSDAY],
                weekdays[Calendar.FRIDAY],
                weekdays[Calendar.SATURDAY],
                weekdays[Calendar.SUNDAY]
            ).map { it.replaceFirstChar { char -> char.titlecase(spanishDateLocale) } }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticatedSession()) {
            return
        }

        setContentView(R.layout.activity_reservar)

        monthLabel = findViewById(R.id.tvMonth)
        gridWeekdays = findViewById(R.id.gridWeekdays)
        gridDays = findViewById(R.id.gridDays)
        continueButton = findViewById(R.id.btnContinuarConFecha)
        flowStatusText = findViewById(R.id.tvFlowStatus)

        findViewById<TextView>(R.id.btnPrevMonth).setOnClickListener {
            visibleMonth.add(Calendar.MONTH, -1)
            renderCalendar()
        }
        findViewById<TextView>(R.id.btnNextMonth).setOnClickListener {
            visibleMonth.add(Calendar.MONTH, 1)
            renderCalendar()
        }

        updateButtonsState()
        renderWeekHeaders()
        renderCalendar()

        continueButton.setOnClickListener {
            val reserva = ReservasRepository.obtenerReservaPorFecha(selectedDateMillis)
            val intent = if (reserva != null) {
                DetalleReservaActivity.editIntent(this, reserva)
            } else {
                DetalleReservaActivity.createIntent(this, selectedDateMillis)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!ensureAuthenticatedSession()) {
            return
        }

        validarPerfilAntesDeReservar { puedeReservar ->
            if (!puedeReservar) {
                finish()
                return@validarPerfilAntesDeReservar
            }

            BookingAvailabilityRepository.cargarConfiguracion { _, _ ->
                ReservasRepository.cargarReservasUsuario { ok ->
                    if (!ok) {
                        Toast.makeText(this, R.string.error_cargar_reservas, Toast.LENGTH_SHORT).show()
                    }
                    refreshCurrentDateRange()
                    renderCalendar()
                }
            }
        }
    }


    private fun validarPerfilAntesDeReservar(onResult: (Boolean) -> Unit) {
        PerfilRepository.cargarPerfil { perfil ->
            runOnUiThread {
                val completo = perfil?.estaCompleto() == true
                if (!completo) {
                    Toast.makeText(this, R.string.profile_required_for_booking, Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, PerfilDatosPersonalesActivity::class.java).apply {
                        putExtra(EXTRA_OPENED_FROM_BOOKING, true)
                    })
                    onResult(false)
                    return@runOnUiThread
                }
                onResult(true)
            }
        }
    }

    private fun refreshCurrentDateRange() {
        today = Calendar.getInstance().clearTime()
        val config = BookingAvailabilityRepository.obtenerConfiguracionActual()
        minReservableDate = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, config.initialDelayDays)
        }
        maxReservableDate = (minReservableDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, config.windowLengthDays - 1)
        }
        reservedDates = ReservasRepository.obtenerFechasReservadas()
        if (selectedDateMillis < today.timeInMillis || selectedDateMillis > maxReservableDate.timeInMillis) {
            selectedDateMillis = today.timeInMillis
            hasUserSelectedDate = false
        }
        updateButtonsState()
    }

    private fun updateButtonsState() {
        val hasExistingReservation = selectedDateMillis in reservedDates
        val canCreate = ReservasRepository.puedeCrearReservaEnFecha(selectedDateMillis)
        val canEdit = hasExistingReservation && ReservasRepository.puedeEditarReservaExistenteEnFecha(selectedDateMillis)
        val canContinue = hasUserSelectedDate && (canCreate || canEdit)

        continueButton.isEnabled = canContinue
        continueButton.alpha = if (canContinue) 1f else 0.85f
        continueButton.text = getString(
            if (hasExistingReservation && canEdit) R.string.editar_reserva else R.string.reservar
        )
        continueButton.setBackgroundResource(
            if (canContinue) R.drawable.bg_button_orange else R.drawable.bg_button_gray
        )

        flowStatusText.text = getString(
            when {
                hasExistingReservation && canEdit -> R.string.reserva_estado_editar
                canCreate -> R.string.reserva_estado_crear
                else -> R.string.reserva_estado_fecha_no_disponible
            }
        )
    }

    private fun renderWeekHeaders() {
        gridWeekdays.removeAllViews()
        weekdayLabels.forEach { day ->
            val dayText = TextView(this).apply {
                text = day
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                alpha = 0.85f
                textSize = 20f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = LinearLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            gridWeekdays.addView(dayText)
        }
    }

    private fun renderCalendar() {
        monthLabel.text = monthFormatter.format(visibleMonth.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(spanishDateLocale) else it.toString()
        }

        gridDays.removeAllViews()

        val firstDayOfMonth = (visibleMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val leadingSpaces = (firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        val totalDays = firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)

        repeat(42) { index ->
            val dateNumber = index - leadingSpaces + 1
            if (dateNumber in 1..totalDays) {
                val dayDate = (visibleMonth.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, dateNumber)
                }.clearTime()
                gridDays.addView(createDayCell(dayDate))
            } else {
                gridDays.addView(createEmptyCell())
            }
        }
    }

    private fun createDayCell(dayDate: Calendar): View {
        val isReserved = dayDate.timeInMillis in reservedDates
        val canCreate = ReservasRepository.puedeCrearReservaEnFecha(dayDate.timeInMillis)
        val canEdit = isReserved && ReservasRepository.puedeEditarReservaExistenteEnFecha(dayDate.timeInMillis)
        val isActionable = canCreate || canEdit
        val isSelected = hasUserSelectedDate && dayDate.timeInMillis == selectedDateMillis
        val isToday = dayDate.timeInMillis == today.timeInMillis

        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4.dp(), 4.dp(), 4.dp(), 4.dp())
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 56.dp()
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            setOnClickListener {
                if (!isActionable) return@setOnClickListener

                val tappedDateMillis = dayDate.timeInMillis
                val now = System.currentTimeMillis()
                val isDoubleTap = lastTappedDateMillis == tappedDateMillis && now - lastDateTapTimestamp <= DOUBLE_TAP_WINDOW_MS

                lastTappedDateMillis = tappedDateMillis
                lastDateTapTimestamp = now
                selectedDateMillis = tappedDateMillis
                hasUserSelectedDate = true
                updateButtonsState()
                renderCalendar()

                if (isDoubleTap && continueButton.isEnabled) {
                    continueButton.post { continueButton.performClick() }
                }
            }
        }

        val marker = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp(), 4.dp()).apply {
                bottomMargin = 4.dp()
            }
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            visibility = if (isActionable) View.VISIBLE else View.INVISIBLE
            setBackgroundColor(if (canEdit) 0xFFE67E22.toInt() else 0xFF3FC77AL.toInt())
        }

        val dayText = TextView(this).apply {
            text = dayDate.get(Calendar.DAY_OF_MONTH).toString()
            gravity = Gravity.CENTER
            textSize = 24f
            setTextColor(
                when {
                    isSelected -> 0xFFF6F1E4.toInt()
                    canEdit -> 0xFFFFE0BF.toInt()
                    canCreate -> 0xFFDAECCE.toInt()
                    else -> 0xFFE8CF9F.toInt()
                }
            )
            layoutParams = LinearLayout.LayoutParams(40.dp(), 40.dp())
            background = ContextCompat.getDrawable(
                context,
                when {
                    isSelected -> R.drawable.bg_day_selected
                    canEdit -> R.drawable.bg_day_reserved
                    isToday -> R.drawable.bg_day_today
                    canCreate -> R.drawable.bg_day_available
                    else -> R.drawable.bg_day_default
                }
            )
        }

        cell.alpha = if (isActionable || isToday) 1f else 0.6f
        cell.addView(marker)
        cell.addView(dayText)
        return cell
    }

    private fun createEmptyCell(): View = View(this).apply {
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 56.dp()
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        }
    }

    private fun Calendar.clearTime(): Calendar = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val DOUBLE_TAP_WINDOW_MS = 400L
        const val EXTRA_OPENED_FROM_BOOKING = "opened_from_booking"
    }
}
