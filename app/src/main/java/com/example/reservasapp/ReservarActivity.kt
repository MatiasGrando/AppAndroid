package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservarActivity : AppCompatActivity() {
    private lateinit var monthLabel: TextView
    private lateinit var gridWeekdays: GridLayout
    private lateinit var gridDays: GridLayout
    private lateinit var continueButton: Button

    private var today: Calendar = Calendar.getInstance().clearTime()
    private var maxReservableDate: Calendar = Calendar.getInstance().clearTime().apply {
        add(Calendar.DAY_OF_YEAR, 6)
    }

    private var visibleMonth: Calendar = Calendar.getInstance().clearTime().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }
    private var selectedDateMillis: Long = today.timeInMillis
    private var reservedDates: Set<Long> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservar)

        monthLabel = findViewById(R.id.tvMonth)
        gridWeekdays = findViewById(R.id.gridWeekdays)
        gridDays = findViewById(R.id.gridDays)
        continueButton = findViewById(R.id.btnContinuar)

        findViewById<TextView>(R.id.btnPrevMonth).setOnClickListener {
            visibleMonth.add(Calendar.MONTH, -1)
            renderCalendar()
        }
        findViewById<TextView>(R.id.btnNextMonth).setOnClickListener {
            visibleMonth.add(Calendar.MONTH, 1)
            renderCalendar()
        }

        renderWeekHeaders()
        renderCalendar()

        continueButton.setOnClickListener {
            startActivity(Intent(this, DetalleReservaActivity::class.java).apply {
                putExtra(DetalleReservaActivity.EXTRA_DATE_MILLIS, selectedDateMillis)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        validarPerfilAntesDeReservar { puedeReservar ->
            if (!puedeReservar) {
                finish()
                return@validarPerfilAntesDeReservar
            }

            ReservasRepository.cargarReservasUsuario { ok ->
                if (!ok) {
                    Toast.makeText(this, R.string.error_cargar_reservas, Toast.LENGTH_SHORT).show()
                }
                refreshCurrentDateRange()
                renderCalendar()
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
        maxReservableDate = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 6)
        }
        reservedDates = ReservasRepository.obtenerFechasReservadas()
        if (selectedDateMillis < today.timeInMillis || selectedDateMillis > maxReservableDate.timeInMillis) {
            selectedDateMillis = today.timeInMillis
        }
        updateContinueButtonState()
    }

    private fun updateContinueButtonState() {
        val isReservable = selectedDateMillis in today.timeInMillis..maxReservableDate.timeInMillis
        val hasExistingReservation = selectedDateMillis in reservedDates
        val canContinue = isReservable && !hasExistingReservation

        continueButton.isEnabled = canContinue
        continueButton.alpha = if (canContinue) 1f else 0.85f
        continueButton.setBackgroundResource(
            if (canContinue) R.drawable.bg_button_orange else R.drawable.bg_button_gray
        )
    }

    private fun renderWeekHeaders() {
        gridWeekdays.removeAllViews()
        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        dayLabels.forEach { day ->
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
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        monthLabel.text = formatter.format(visibleMonth.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
        }

        gridDays.removeAllViews()

        val firstDayOfMonth = (visibleMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val leadingSpaces = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
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
        val isReservable = dayDate.timeInMillis in today.timeInMillis..maxReservableDate.timeInMillis
        val isReserved = dayDate.timeInMillis in reservedDates
        val isSelected = dayDate.timeInMillis == selectedDateMillis
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
                if (!isReservable) return@setOnClickListener
                selectedDateMillis = dayDate.timeInMillis
                updateContinueButtonState()
                renderCalendar()
            }
        }

        val marker = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(24.dp(), 4.dp()).apply {
                bottomMargin = 4.dp()
            }
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
            visibility = if (isReservable) View.VISIBLE else View.INVISIBLE
            setBackgroundColor(if (isReserved) 0xFFE67E22.toInt() else 0xFF3FC77AL.toInt())
        }

        val dayText = TextView(this).apply {
            text = dayDate.get(Calendar.DAY_OF_MONTH).toString()
            gravity = Gravity.CENTER
            textSize = 24f
            setTextColor(
                when {
                    isSelected -> 0xFFF9E5B4.toInt()
                    isReserved -> 0xFFFFE0BF.toInt()
                    isReservable -> 0xFFDAECCE.toInt()
                    else -> 0xFFE8CF9F.toInt()
                }
            )
            layoutParams = LinearLayout.LayoutParams(40.dp(), 40.dp())
            background = ContextCompat.getDrawable(
                context,
                when {
                    isSelected -> R.drawable.bg_day_selected
                    isReserved -> R.drawable.bg_day_reserved
                    isToday -> R.drawable.bg_day_today
                    isReservable -> R.drawable.bg_day_available
                    else -> R.drawable.bg_day_default
                }
            )
        }

        cell.alpha = if (isReservable || isToday) 1f else 0.6f
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
        const val EXTRA_OPENED_FROM_BOOKING = "opened_from_booking"
    }
}
