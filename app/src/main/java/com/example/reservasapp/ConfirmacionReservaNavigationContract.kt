package com.example.reservasapp

import android.content.Context
import android.content.Intent
import com.example.reservasapp.booking.BookingConfirmationNavigationData
import com.example.reservasapp.booking.BookingConfirmationRawRequest

/**
 * Traduce la navegacion a confirmacion a un Intent estable para que detalle no conozca el mapa
 * completo de extras ni repita el contrato entre pantallas.
 */
fun confirmacionReservaIntent(
    context: Context,
    navigation: BookingConfirmationNavigationData
): Intent {
    return Intent(context, ConfirmacionReservaActivity::class.java).apply {
        putExtra(ConfirmacionReservaActivity.EXTRA_FECHA, navigation.fechaFormateada)
        putExtra(ConfirmacionReservaActivity.EXTRA_FECHA_MILLIS, navigation.fechaMillis)
        putExtra(ConfirmacionReservaActivity.EXTRA_DETALLE, navigation.detalleSeleccion)
        putExtra(
            ConfirmacionReservaActivity.EXTRA_SELECCIONES_PENDIENTES,
            navigation.seleccionesPendientes
        )
        putExtra(ConfirmacionReservaActivity.EXTRA_RESERVA_ID, navigation.reservaId)
        putExtra(ConfirmacionReservaActivity.EXTRA_ES_EDICION, navigation.esEdicion)
    }
}

/**
 * Reconstruye el request crudo desde extras legacy para mantener compatibilidad sin acoplar la
 * Activity a cada clave del Intent.
 */
fun Intent.toConfirmacionReservaRawRequest(): BookingConfirmationRawRequest {
    @Suppress("DEPRECATION")
    val seleccionesPendientesRaw = (getSerializableExtra(
        ConfirmacionReservaActivity.EXTRA_SELECCIONES_PENDIENTES
    ) as? Map<*, *>)
        ?.mapNotNull { (key, value) ->
            val safeKey = key as? String ?: return@mapNotNull null
            val safeValue = value as? String ?: return@mapNotNull null
            safeKey to safeValue
        }
        ?.toMap()
        .orEmpty()

    return BookingConfirmationRawRequest(
        esEdicion = getBooleanExtra(ConfirmacionReservaActivity.EXTRA_ES_EDICION, false),
        fecha = getStringExtra(ConfirmacionReservaActivity.EXTRA_FECHA).orEmpty(),
        detalleSeleccion = getStringExtra(ConfirmacionReservaActivity.EXTRA_DETALLE).orEmpty(),
        reservaId = getStringExtra(ConfirmacionReservaActivity.EXTRA_RESERVA_ID).orEmpty(),
        fechaMillis = getLongExtra(ConfirmacionReservaActivity.EXTRA_FECHA_MILLIS, -1L),
        hasSeleccionesPendientesExtra = hasExtra(ConfirmacionReservaActivity.EXTRA_SELECCIONES_PENDIENTES),
        seleccionesPendientesRaw = seleccionesPendientesRaw
    )
}
