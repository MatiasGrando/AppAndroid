package com.example.reservasapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.example.reservasapp.booking.BookingCalendarLoadResult
import com.example.reservasapp.booking.BookingCalendarState
import com.example.reservasapp.booking.BookingDetailDestination
import com.example.reservasapp.booking.BookingFlowService
import com.example.reservasapp.branding.AppRuntime
import android.widget.Toast
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Pantalla de entrada al flujo de reservas: muestra la ventana disponible y deriva a crear o editar.
 */
class ReservarActivity : BaseActivity() {
    private lateinit var monthLabel: TextView
    private lateinit var gridWeekdays: GridLayout
    private lateinit var gridDays: GridLayout
    private lateinit var calendarCard: CardView
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

        calendarCard = findViewById(R.id.calendarCard)
        monthLabel = findViewById(R.id.tvMonth)
        gridWeekdays = findViewById(R.id.gridWeekdays)
        gridDays = findViewById(R.id.gridDays)
        continueButton = findViewById(R.id.btnContinuarConFecha)
        flowStatusText = findViewById(R.id.tvFlowStatus)

        applyBranding()

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
            navegarADetalleSeleccionado()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!ensureAuthenticatedSession()) {
            return
        }

        BookingFlowService.cargarEstadoCalendarioInicial(selectedDateMillis, hasUserSelectedDate) { result ->
            runOnUiThread {
                when (result) {
                    BookingCalendarLoadResult.ProfileIncomplete -> {
                        Toast.makeText(this, R.string.profile_required_for_booking, Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, PerfilDatosPersonalesActivity::class.java).apply {
                            putExtra(EXTRA_OPENED_FROM_BOOKING, true)
                        })
                        finish()
                    }

                    is BookingCalendarLoadResult.Ready -> {
                        if (!result.reservasLoaded) {
                            Toast.makeText(this, R.string.error_cargar_reservas, Toast.LENGTH_SHORT).show()
                        }
                        applyCalendarState(result.state)
                        renderCalendar()
                    }
                }
            }
        }
    }

    /**
     * Aplica al estado local el resultado calculado por el servicio para no duplicar reglas de ventana.
     */
    private fun applyCalendarState(state: BookingCalendarState) {
        today = Calendar.getInstance().clearTime().apply { timeInMillis = state.todayMillis }
        minReservableDate = Calendar.getInstance().clearTime().apply { timeInMillis = state.minReservableDateMillis }
        maxReservableDate = Calendar.getInstance().clearTime().apply { timeInMillis = state.maxReservableDateMillis }
        selectedDateMillis = state.selectedDateMillis
        hasUserSelectedDate = state.hasUserSelectedDate
        updateButtonsState()
    }

    /**
     * Decide alta o edicion a traves del servicio para que la Activity no consulte directo el repository.
     */
    private fun navegarADetalleSeleccionado() {
        val destination = BookingFlowService.resolverDestinoDetalle(selectedDateMillis) ?: return
        val intent = when (destination) {
            is BookingDetailDestination.Create -> {
                DetalleReservaActivity.createIntent(this, destination.selectedDateMillis)
            }

            is BookingDetailDestination.Edit -> {
                DetalleReservaActivity.editIntent(this, destination.reserva)
            }
        }
        startActivity(intent)
    }

    /**
     * Traduce el estado de negocio de la fecha seleccionada al copy y habilitacion del CTA.
     */
    private fun updateButtonsState() {
        val branding = AppRuntime.branding
        val dateAvailability = BookingFlowService.resolverAccionFecha(selectedDateMillis)
        val hasExistingReservation = dateAvailability.reservaExistente != null
        val canCreate = dateAvailability.canCreate
        val canEdit = dateAvailability.canEdit
        val canContinue = hasUserSelectedDate && dateAvailability.canContinue
        val buttonColor = ContextCompat.getColor(
            this,
            when {
                !canContinue -> branding.secondaryActionColorRes
                hasExistingReservation && canEdit -> branding.secondaryActionColorRes
                else -> branding.primaryActionColorRes
            }
        )

        continueButton.isEnabled = canContinue
        continueButton.alpha = if (canContinue) 1f else 0.85f
        continueButton.text = getString(
            if (hasExistingReservation && canEdit) R.string.editar_reserva else R.string.reservar
        )
        continueButton.backgroundTintList = ColorStateList.valueOf(buttonColor)
        continueButton.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))

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
        val weekdayColor = ContextCompat.getColor(this, AppRuntime.branding.confirmationBodyTextColorRes)

        weekdayLabels.forEach { day ->
            val dayText = TextView(this).apply {
                text = day
                setTextColor(weekdayColor)
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

    /**
     * Pinta una celda del calendario segun disponibilidad y reserva existente para esa fecha.
     */
    private fun createDayCell(dayDate: Calendar): View {
        val branding = AppRuntime.branding
        val dateAvailability = BookingFlowService.resolverAccionFecha(dayDate.timeInMillis)
        val canCreate = dateAvailability.canCreate
        val canEdit = dateAvailability.canEdit
        val isActionable = dateAvailability.canContinue
        val isSelected = hasUserSelectedDate && dayDate.timeInMillis == selectedDateMillis
        val isToday = dayDate.timeInMillis == today.timeInMillis
        val actionTextColor = ContextCompat.getColor(this, branding.actionTextColorRes)
        val titleColor = ContextCompat.getColor(this, branding.confirmationTitleColorRes)
        val bodyColor = ContextCompat.getColor(this, branding.confirmationBodyTextColorRes)
        val mutedBodyColor = ColorUtils.setAlphaComponent(bodyColor, (255 * 0.6f).toInt())
        val createMarkerColor = ContextCompat.getColor(this, branding.primaryActionColorRes)
        val editMarkerColor = ContextCompat.getColor(this, branding.secondaryActionColorRes)

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
            setBackgroundColor(if (canEdit) editMarkerColor else createMarkerColor)
        }

        val dayText = TextView(this).apply {
            text = dayDate.get(Calendar.DAY_OF_MONTH).toString()
            gravity = Gravity.CENTER
            textSize = 24f
            setTextColor(
                when {
                    isSelected -> actionTextColor
                    canEdit -> titleColor
                    canCreate -> bodyColor
                    else -> mutedBodyColor
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

    private fun applyBranding() {
        val branding = AppRuntime.branding
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val titleColor = ContextCompat.getColor(this, branding.confirmationTitleColorRes)
        val bodyColor = ContextCompat.getColor(this, branding.confirmationBodyTextColorRes)

        root.setBackgroundResource(branding.homeBackgroundRes)
        calendarCard.setCardBackgroundColor(
            ContextCompat.getColor(this, branding.confirmationCardBackgroundColorRes)
        )

        findViewById<TextView>(R.id.tvSeleccionaDia).setTextColor(titleColor)
        monthLabel.setTextColor(titleColor)
        findViewById<TextView>(R.id.btnPrevMonth).setTextColor(titleColor)
        findViewById<TextView>(R.id.btnNextMonth).setTextColor(titleColor)
        flowStatusText.setTextColor(bodyColor)
        continueButton.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
        title = getString(branding.appNameRes)
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
