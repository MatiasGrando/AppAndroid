package com.example.reservasapp.booking

import com.example.reservasapp.MenuDish
import com.example.reservasapp.MenuIdentity
import com.example.reservasapp.MenuRepository
import com.example.reservasapp.MenuSection
import com.example.reservasapp.Reserva
import com.example.reservasapp.ReservasRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Agrupa la logica de menu/preview para que el flujo principal no mezcle calendario,
 * navegacion y composicion visual en una sola fachada.
 */
object BookingMenuCoordinator {

    /**
     * Expone el menu cacheado para pintar rapido sin volver a acoplar Activities al repository.
     */
    fun obtenerMenuCache(): List<MenuSection> = MenuRepository.obtenerSeccionesCache()

    /**
     * Trae el menu vigente para la fecha elegida y concentra en un punto la dependencia con MenuRepository.
     */
    fun cargarMenuParaDetalle(
        selectedDateMillis: Long,
        onComplete: (Boolean, List<MenuSection>) -> Unit
    ) {
        MenuRepository.cargarSecciones(selectedDateMillis, onComplete)
    }

    /**
     * Decide si la guarnicion sigue habilitada segun el principal ya elegido.
     */
    fun estaGuarnicionHabilitada(
        secciones: List<MenuSection>,
        selecciones: Map<String, String?>
    ): Boolean {
        val principal = secciones.firstOrNull { it.id == MenuIdentity.SECTION_MAIN }
            ?: return false
        val seleccionPrincipal = selecciones[principal.id] ?: return false
        return principal.opciones.firstOrNull { it.id == seleccionPrincipal }?.guarnicion == true
    }

    /**
     * Arma los extras de confirmacion con selecciones saneadas y fecha legible.
     */
    fun crearNavegacionConfirmacion(
        selectedDateMillis: Long,
        reservaEnEdicion: Reserva?,
        selecciones: Map<String, String?>
    ): BookingConfirmationNavigationData {
        val seleccionesFinales = selecciones
            .filterValues { !it.isNullOrBlank() }
            .mapValues { it.value.orEmpty() }

        return BookingConfirmationNavigationData(
            fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(selectedDateMillis)),
            fechaMillis = selectedDateMillis,
            detalleSeleccion = ReservasRepository.formatearSelecciones(seleccionesFinales),
            seleccionesPendientes = HashMap(seleccionesFinales),
            reservaId = reservaEnEdicion?.id.orEmpty(),
            esEdicion = reservaEnEdicion != null
        )
    }

    /**
     * Construye la vista previa de confirmacion resolviendo nombres e imagenes del menu actual.
     */
    fun cargarVistaPreviaConfirmacion(
        request: BookingConfirmationRequest,
        onComplete: (Boolean, BookingConfirmationPreview) -> Unit
    ) {
        val cachedSections = MenuRepository.obtenerSecciones()
        if (cachedSections.isNotEmpty()) {
            onComplete(true, construirVistaPreviaConfirmacion(request, cachedSections))
            return
        }

        MenuRepository.cargarSecciones { ok, secciones ->
            onComplete(ok, construirVistaPreviaConfirmacion(request, secciones))
        }
    }

    private fun construirVistaPreviaConfirmacion(
        request: BookingConfirmationRequest,
        secciones: List<MenuSection>
    ): BookingConfirmationPreview {
        val seleccionesVisualizadas = if (request.seleccionesPendientes.isNotEmpty()) {
            request.seleccionesPendientes
        } else {
            request.reserva?.selecciones.orEmpty()
        }

        val seccionesPorId = secciones.associateBy { it.id }
        val imageUrlsByDishId = secciones
            .flatMap { it.opciones }
            .associateBy(
                keySelector = MenuDish::id,
                valueTransform = MenuDish::imageUrl
            )

        val principalId = seleccionesVisualizadas[MenuIdentity.SECTION_MAIN]
        val guarnicionId = seleccionesVisualizadas[MenuIdentity.SECTION_SIDE]
        val postreId = seleccionesVisualizadas[MenuIdentity.SECTION_DESSERT]

        return BookingConfirmationPreview(
            principalId = principalId,
            guarnicionId = guarnicionId,
            postreId = postreId,
            principal = seccionesPorId[MenuIdentity.SECTION_MAIN]
                ?.opciones
                ?.firstOrNull { it.id == principalId }
                ?.nombre
                ?: principalId,
            guarnicion = seccionesPorId[MenuIdentity.SECTION_SIDE]
                ?.opciones
                ?.firstOrNull { it.id == guarnicionId }
                ?.nombre
                ?: guarnicionId,
            postre = seccionesPorId[MenuIdentity.SECTION_DESSERT]
                ?.opciones
                ?.firstOrNull { it.id == postreId }
                ?.nombre
                ?: postreId,
            imageUrlsByDishId = imageUrlsByDishId
        )
    }
}
