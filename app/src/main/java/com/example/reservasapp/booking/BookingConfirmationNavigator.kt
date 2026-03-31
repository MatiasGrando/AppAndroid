package com.example.reservasapp.booking

import android.content.Context
import android.content.Intent
import com.example.reservasapp.Reserva
import com.example.reservasapp.confirmacionReservaIntent

/**
 * Encapsula el salto a confirmacion para que detalle entregue solo el contexto del flujo
 * y no conozca el payload final ni el contrato completo de extras.
 */
object BookingConfirmationNavigator {

    fun createIntent(
        context: Context,
        route: BookingConfirmationRoute
    ): Intent {
        val navigation = BookingMenuCoordinator.crearNavegacionConfirmacion(
            selectedDateMillis = route.selectedDateMillis,
            reservaEnEdicion = route.reservaEnEdicion,
            selecciones = route.selecciones
        )

        return confirmacionReservaIntent(context, navigation)
    }
}

/**
 * Entrada minima que detalle necesita aportar para navegar a confirmacion.
 */
data class BookingConfirmationRoute(
    val selectedDateMillis: Long,
    val reservaEnEdicion: Reserva?,
    val selecciones: Map<String, String?>
)
