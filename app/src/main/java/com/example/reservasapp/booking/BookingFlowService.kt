package com.example.reservasapp.booking

import androidx.annotation.StringRes
import com.example.reservasapp.BookingAvailabilityRepository
import com.example.reservasapp.PerfilRepository
import com.example.reservasapp.R
import com.example.reservasapp.Reserva
import com.example.reservasapp.ReservasRepository
import com.example.reservasapp.estaCompleto
import com.example.reservasapp.resolveConfirmacionReservaRequest
import java.util.Calendar

/**
 * Encapsula la orquestacion principal del flujo de reservas para que las Activities
 * se concentren en renderizar estado y navegar, no en hablar directo con repositories.
 */
object BookingFlowService {

    /**
     * Valida el perfil, refresca configuracion/reservas y devuelve el estado del calendario listo para pintar.
     */
    fun cargarEstadoCalendarioInicial(
        selectedDateMillis: Long,
        hasUserSelectedDate: Boolean,
        onComplete: (BookingCalendarLoadResult) -> Unit
    ) {
        PerfilRepository.cargarPerfil { perfil ->
            val perfilCompleto = perfil?.estaCompleto() == true
            if (!perfilCompleto) {
                onComplete(BookingCalendarLoadResult.ProfileIncomplete)
                return@cargarPerfil
            }

            BookingAvailabilityRepository.cargarConfiguracion { _, _ ->
                ReservasRepository.cargarReservasUsuario { reservasOk ->
                    onComplete(
                        BookingCalendarLoadResult.Ready(
                            state = resolverEstadoCalendario(
                                selectedDateMillis = selectedDateMillis,
                                hasUserSelectedDate = hasUserSelectedDate
                            ),
                            reservasLoaded = reservasOk
                        )
                    )
                }
            }
        }
    }

    /**
     * Calcula la ventana vigente y corrige selecciones vencidas para que la UI no decida reglas de negocio.
     */
    fun resolverEstadoCalendario(
        selectedDateMillis: Long,
        hasUserSelectedDate: Boolean
    ): BookingCalendarState {
        val today = Calendar.getInstance().clearTime()
        val config = BookingAvailabilityRepository.obtenerConfiguracionActual()
        val minReservableDate = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, config.initialDelayDays)
        }
        val maxReservableDate = (minReservableDate.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, config.windowLengthDays - 1)
        }

        val selectionStillValid = selectedDateMillis in today.timeInMillis..maxReservableDate.timeInMillis
        return BookingCalendarState(
            todayMillis = today.timeInMillis,
            minReservableDateMillis = minReservableDate.timeInMillis,
            maxReservableDateMillis = maxReservableDate.timeInMillis,
            selectedDateMillis = if (selectionStillValid) selectedDateMillis else today.timeInMillis,
            hasUserSelectedDate = hasUserSelectedDate && selectionStillValid
        )
    }

    /**
     * Resume si la fecha permite crear, editar o bloquear el avance sin exponer reglas del repository a la Activity.
     */
    fun resolverAccionFecha(selectedDateMillis: Long): BookingDateAvailability {
        val reservaExistente = ReservasRepository.obtenerReservaPorFecha(selectedDateMillis)
        val canCreate = ReservasRepository.puedeCrearReservaEnFecha(selectedDateMillis)
        val canEdit = reservaExistente != null &&
            ReservasRepository.puedeEditarReservaExistenteEnFecha(selectedDateMillis)

        return BookingDateAvailability(
            reservaExistente = reservaExistente,
            canCreate = canCreate,
            canEdit = canEdit
        )
    }

    /**
     * Define si el flujo entra en modo alta o edicion para una fecha puntual.
     */
    fun resolverDestinoDetalle(selectedDateMillis: Long): BookingDetailDestination? {
        val availability = resolverAccionFecha(selectedDateMillis)
        return when {
            availability.canEdit && availability.reservaExistente != null -> BookingDetailDestination.Edit(
                availability.reservaExistente
            )

            availability.canCreate -> BookingDetailDestination.Create(selectedDateMillis)
            else -> null
        }
    }

    /**
     * Valida el punto de entrada a detalle para evitar que la Activity replique chequeos de permisos/estado.
     */
    fun resolverEntradaDetalle(
        reservaId: String,
        selectedDateMillis: Long?,
        hasDateExtra: Boolean
    ): BookingDetailEntryResolution {
        if (reservaId.isNotBlank()) {
            val reserva = ReservasRepository.obtenerReservaPorId(reservaId)
            return if (reserva == null || !ReservasRepository.puedeEditarReservaExistenteEnFecha(reserva.fechaMillis)) {
                BookingDetailEntryResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
            } else {
                BookingDetailEntryResolution.Valid(
                    BookingDetailEntry(
                        reservaEnEdicion = reserva,
                        selectedDateMillis = reserva.fechaMillis
                    )
                )
            }
        }

        if (!hasDateExtra) {
            return BookingDetailEntryResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
        }

        val safeDateMillis = selectedDateMillis ?: -1L
        return if (safeDateMillis <= 0L || !ReservasRepository.puedeCrearReservaEnFecha(safeDateMillis)) {
            BookingDetailEntryResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
        } else {
            BookingDetailEntryResolution.Valid(
                BookingDetailEntry(
                    reservaEnEdicion = null,
                    selectedDateMillis = safeDateMillis
                )
            )
        }
    }

    /**
     * Interpreta extras de confirmacion y protege el flujo contra intents incompletos o vencidos.
     */
    fun resolverConfirmacion(rawRequest: BookingConfirmationRawRequest): BookingConfirmationRequestResolution {
        return resolveConfirmacionReservaRequest(rawRequest)
    }

    /**
     * Ejecuta la confirmacion final respetando las validaciones actuales del repository.
     */
    fun confirmarReserva(
        request: BookingConfirmationRequest,
        onComplete: (BookingConfirmationSubmitResult) -> Unit
    ) {
        if (request.esEdicion) {
            if (request.reservaId.isBlank()) {
                onComplete(BookingConfirmationSubmitResult.Error(R.string.error_actualizar_reserva))
                return
            }

            ReservasRepository.actualizarReserva(
                id = request.reservaId,
                selecciones = request.seleccionesPendientes
            ) { actualizada ->
                if (actualizada != null) {
                    onComplete(BookingConfirmationSubmitResult.Success)
                } else {
                    onComplete(BookingConfirmationSubmitResult.Error(R.string.error_actualizar_reserva))
                }
            }
            return
        }

        if (request.fechaMillis <= 0L) {
            onComplete(BookingConfirmationSubmitResult.Error(R.string.error_guardar_reserva))
            return
        }

        ReservasRepository.agregarReserva(
            fechaMillis = request.fechaMillis,
            selecciones = request.seleccionesPendientes
        ) { resultado ->
            when {
                resultado.reservaCreada != null -> onComplete(BookingConfirmationSubmitResult.Success)
                resultado.reservaExistente != null -> {
                    onComplete(BookingConfirmationSubmitResult.ExistingReservation)
                }

                else -> onComplete(BookingConfirmationSubmitResult.Error(R.string.error_guardar_reserva))
            }
        }
    }

}

sealed interface BookingCalendarLoadResult {
    data object ProfileIncomplete : BookingCalendarLoadResult
    data class Ready(
        val state: BookingCalendarState,
        val reservasLoaded: Boolean
    ) : BookingCalendarLoadResult
}

data class BookingCalendarState(
    val todayMillis: Long,
    val minReservableDateMillis: Long,
    val maxReservableDateMillis: Long,
    val selectedDateMillis: Long,
    val hasUserSelectedDate: Boolean
)

data class BookingDateAvailability(
    val reservaExistente: Reserva?,
    val canCreate: Boolean,
    val canEdit: Boolean
) {
    val canContinue: Boolean
        get() = canCreate || canEdit
}

sealed interface BookingDetailDestination {
    data class Create(val selectedDateMillis: Long) : BookingDetailDestination
    data class Edit(val reserva: Reserva) : BookingDetailDestination
}

data class BookingDetailEntry(
    val reservaEnEdicion: Reserva?,
    val selectedDateMillis: Long
)

sealed interface BookingDetailEntryResolution {
    data class Valid(val entry: BookingDetailEntry) : BookingDetailEntryResolution
    data class Invalid(@StringRes val messageRes: Int) : BookingDetailEntryResolution
}

data class BookingConfirmationNavigationData(
    val fechaFormateada: String,
    val fechaMillis: Long,
    val detalleSeleccion: String,
    val seleccionesPendientes: HashMap<String, String>,
    val reservaId: String,
    val esEdicion: Boolean
)

data class BookingConfirmationPreview(
    val principalId: String?,
    val guarnicionId: String?,
    val postreId: String?,
    val principal: String?,
    val guarnicion: String?,
    val postre: String?,
    val imageUrlsByDishId: Map<String, String>
)

data class BookingConfirmationRequest(
    val fecha: String,
    val detalleSeleccion: String,
    val reserva: Reserva?,
    val esEdicion: Boolean,
    val fechaMillis: Long,
    val seleccionesPendientes: Map<String, String>
) {
    val reservaId: String
        get() = reserva?.id.orEmpty()
}

data class BookingConfirmationRawRequest(
    val esEdicion: Boolean,
    val fecha: String,
    val detalleSeleccion: String,
    val reservaId: String,
    val fechaMillis: Long,
    val hasSeleccionesPendientesExtra: Boolean,
    val seleccionesPendientesRaw: Map<String, String>
)

sealed interface BookingConfirmationRequestResolution {
    data class Valid(val request: BookingConfirmationRequest) : BookingConfirmationRequestResolution
    data class Invalid(@StringRes val messageRes: Int) : BookingConfirmationRequestResolution
}

sealed interface BookingConfirmationSubmitResult {
    data object Success : BookingConfirmationSubmitResult
    data object ExistingReservation : BookingConfirmationSubmitResult
    data class Error(@StringRes val messageRes: Int) : BookingConfirmationSubmitResult
}

private fun Calendar.clearTime(): Calendar = apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
