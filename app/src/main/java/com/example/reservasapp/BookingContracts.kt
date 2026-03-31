package com.example.reservasapp

import com.example.reservasapp.booking.BookingConfirmationRawRequest
import com.example.reservasapp.booking.BookingConfirmationRequest
import com.example.reservasapp.booking.BookingConfirmationRequestResolution

/**
 * Contrato minimo para navegar a detalle sin arrastrar dependencias de Android a tests de JVM.
 */
data class DetalleReservaNavigationContract(
    val reservaId: String,
    val selectedDateMillis: Long
)

/**
 * Mantiene estable el contrato de alta hacia detalle y le da una sola forma a los extras.
 */
fun detalleReservaNavigationForCreate(dateMillis: Long): DetalleReservaNavigationContract {
    return DetalleReservaNavigationContract(
        reservaId = "",
        selectedDateMillis = dateMillis
    )
}

/**
 * Mantiene estable el contrato de edicion usando la fecha persistida de la reserva.
 */
fun detalleReservaNavigationForEdit(reserva: Reserva): DetalleReservaNavigationContract {
    return DetalleReservaNavigationContract(
        reservaId = reserva.id,
        selectedDateMillis = reserva.fechaMillis
    )
}

typealias ConfirmacionReservaRawRequest = BookingConfirmationRawRequest
typealias ConfirmacionReservaRequestResolution = BookingConfirmationRequestResolution

/**
 * Separa el saneamiento y la validacion del request de confirmacion para que el servicio quede
 * como fachada y los tests sigan cubriendo el contrato sin tocar repositories reales.
 */
fun resolveConfirmacionReservaRequest(
    rawRequest: ConfirmacionReservaRawRequest,
    sanitizeSelecciones: (Map<String, String>) -> Map<String, String> = ReservasRepository::sanitizeSelecciones,
    reservaById: (String) -> Reserva? = ReservasRepository::obtenerReservaPorId,
    canEditReservationDate: (Long) -> Boolean = ReservasRepository::puedeEditarReservaExistenteEnFecha,
    canCreateReservationDate: (Long) -> Boolean = ReservasRepository::puedeCrearReservaEnFecha
): ConfirmacionReservaRequestResolution {
    val seleccionesPendientes = sanitizeSelecciones(rawRequest.seleccionesPendientesRaw)

    if (rawRequest.hasSeleccionesPendientesExtra && seleccionesPendientes.isEmpty()) {
        return BookingConfirmationRequestResolution.Invalid(
            if (rawRequest.esEdicion) R.string.error_actualizar_reserva else R.string.error_guardar_reserva
        )
    }

    if (rawRequest.esEdicion) {
        val reserva = reservaById(rawRequest.reservaId)
        return if (rawRequest.reservaId.isBlank() || reserva == null ||
            !canEditReservationDate(reserva.fechaMillis)
        ) {
            BookingConfirmationRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
        } else {
            BookingConfirmationRequestResolution.Valid(
                BookingConfirmationRequest(
                    fecha = rawRequest.fecha,
                    detalleSeleccion = rawRequest.detalleSeleccion,
                    reserva = reserva,
                    esEdicion = true,
                    fechaMillis = reserva.fechaMillis,
                    seleccionesPendientes = seleccionesPendientes
                )
            )
        }
    }

    return if (rawRequest.fechaMillis <= 0L || !canCreateReservationDate(rawRequest.fechaMillis)) {
        BookingConfirmationRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
    } else {
        BookingConfirmationRequestResolution.Valid(
            BookingConfirmationRequest(
                fecha = rawRequest.fecha,
                detalleSeleccion = rawRequest.detalleSeleccion,
                reserva = null,
                esEdicion = false,
                fechaMillis = rawRequest.fechaMillis,
                seleccionesPendientes = seleccionesPendientes
            )
        )
    }
}
