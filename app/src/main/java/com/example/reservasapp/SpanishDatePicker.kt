package com.example.reservasapp

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar
import java.util.Locale

internal val spanishDateLocale: Locale = Locale.forLanguageTag("es-ES")

internal fun Context.createSpanishDatePickerDialog(
    initialMillis: Long,
    onDateSelected: (Long) -> Unit
): DatePickerDialog {
    val calendar = Calendar.getInstance().clearTime().apply { timeInMillis = initialMillis }

    return DatePickerDialog(
        this,
        { _, year, month, dayOfMonth ->
            onDateSelected(
                Calendar.getInstance().clearTime().apply {
                    set(year, month, dayOfMonth)
                }.timeInMillis
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.firstDayOfWeek = Calendar.MONDAY
    }
}

private fun Calendar.clearTime(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
