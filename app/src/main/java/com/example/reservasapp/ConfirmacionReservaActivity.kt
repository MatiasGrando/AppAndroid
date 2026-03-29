package com.example.reservasapp

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.reservasapp.branding.AppRuntime
import com.example.reservasapp.firebase.FirebaseProvider
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfirmacionReservaActivity : BaseActivity() {

    private val storage by lazy { FirebaseProvider.storage() }
    private var imageByDishId: Map<String, String> = emptyMap()

    companion object {
        const val EXTRA_FECHA = "extra_fecha"
        const val EXTRA_DETALLE = "extra_detalle"
        const val EXTRA_RESERVA_ID = "extra_reserva_id"
        const val EXTRA_ES_EDICION = "extra_es_edicion"
        const val EXTRA_FECHA_MILLIS = "extra_fecha_millis"
        const val EXTRA_SELECCIONES_PENDIENTES = "extra_selecciones_pendientes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticatedSession()) {
            return
        }

        val request = resolveRequest() ?: run {
            finish()
            return
        }

        setContentView(R.layout.activity_confirmacion_reserva)

        val fecha = request.fecha
        val detalleSeleccion = request.detalleSeleccion
        val reservaId = request.reservaId
        val esEdicion = request.esEdicion
        val fechaMillis = request.fechaMillis
        val seleccionesPendientes = request.seleccionesPendientes
        val reserva = request.reserva

        val titulo = findViewById<TextView>(R.id.tvTituloConfirmacion)
        val detalle = findViewById<TextView>(R.id.tvDetalleConfirmacion)
        val tvFechaReserva = findViewById<TextView>(R.id.tvFechaReserva)
        val tvPrincipal = findViewById<TextView>(R.id.tvPlatoPrincipalNombre)
        val tvGuarnicion = findViewById<TextView>(R.id.tvGuarnicionNombre)
        val tvPostre = findViewById<TextView>(R.id.tvPostreNombre)
        val ivPrincipal = findViewById<ImageView>(R.id.ivPlatoPrincipal)
        val ivGuarnicion = findViewById<ImageView>(R.id.ivGuarnicion)
        val ivPostre = findViewById<ImageView>(R.id.ivPostre)
        val cardPrincipal = findViewById<MaterialCardView>(R.id.cardPlatoPrincipal)
        val cardGuarnicion = findViewById<View>(R.id.cardGuarnicion)
        val cardGuarnicionMaterial = findViewById<MaterialCardView>(R.id.cardGuarnicion)
        val cardPostre = findViewById<MaterialCardView>(R.id.cardPostre)
        val accionPrincipal = findViewById<Button>(R.id.btnVolverMenu)

        applyBranding(
            titulo = titulo,
            detalle = detalle,
            fecha = tvFechaReserva,
            principal = tvPrincipal,
            guarnicion = tvGuarnicion,
            postre = tvPostre,
            accionPrincipal = accionPrincipal,
            cards = listOf(cardPrincipal, cardGuarnicionMaterial, cardPostre)
        )

        titulo.text = getString(
            if (esEdicion) R.string.titulo_confirmacion_edicion else R.string.titulo_confirmacion
        )

        val seleccionesVisualizadas = if (seleccionesPendientes.isNotEmpty()) {
            seleccionesPendientes
        } else {
            reserva?.selecciones.orEmpty()
        }
        val principalId = seleccionesVisualizadas[MenuIdentity.SECTION_MAIN]
        val guarnicionId = seleccionesVisualizadas[MenuIdentity.SECTION_SIDE]
        val postreId = seleccionesVisualizadas[MenuIdentity.SECTION_DESSERT]
        val principal = MenuRepository.nombrePlato(MenuIdentity.SECTION_MAIN, principalId)
        val guarnicion = MenuRepository.nombrePlato(MenuIdentity.SECTION_SIDE, guarnicionId)
        val postre = MenuRepository.nombrePlato(MenuIdentity.SECTION_DESSERT, postreId)

        val formatter = SimpleDateFormat("EEEE d/M/yy", Locale("es", "ES"))
        val fechaVisual = when {
            reserva != null -> Date(reserva.fechaMillis)
            fechaMillis > 0L -> Date(fechaMillis)
            else -> null
        }

        tvFechaReserva.text = fechaVisual?.let {
            formatter.format(it).uppercase(Locale("es", "ES"))
        } ?: fecha.uppercase(Locale("es", "ES"))

        val tieneGuarnicion = !guarnicion.isNullOrBlank()

        tvPrincipal.text = principal ?: "-"
        tvGuarnicion.text = guarnicion.orEmpty()
        tvPostre.text = postre ?: "-"
        cardGuarnicion.visibility = if (tieneGuarnicion) View.VISIBLE else View.GONE

        ivPrincipal.setImageResource(imageForSelection(principal))
        ivGuarnicion.setImageResource(imageForSelection(guarnicion))
        ivPostre.setImageResource(imageForSelection(postre))

        cargarImagenesMenuDesdeStorage(
            principalId = principalId,
            guarnicionId = guarnicionId,
            postreId = postreId,
            ivPrincipal = ivPrincipal,
            ivGuarnicion = ivGuarnicion,
            ivPostre = ivPostre
        )

        if (reserva != null || seleccionesPendientes.isNotEmpty()) {
            detalle.visibility = View.GONE
        } else {
            detalle.visibility = View.VISIBLE
            detalle.text = getString(R.string.resumen_reserva_generico, fecha, detalleSeleccion)
        }

        val hayConfirmacionPendiente = seleccionesPendientes.isNotEmpty()
        accionPrincipal.text = getString(
            if (hayConfirmacionPendiente) R.string.confirmar_pedido else R.string.volver_menu
        )

        accionPrincipal.setOnClickListener {
            if (!hayConfirmacionPendiente) {
                volverAMenuPrincipal()
                return@setOnClickListener
            }

            accionPrincipal.isEnabled = false
            val selecciones = seleccionesPendientes

            if (esEdicion) {
                if (reservaId.isBlank()) {
                    Toast.makeText(this, R.string.error_actualizar_reserva, Toast.LENGTH_LONG).show()
                    accionPrincipal.isEnabled = true
                    return@setOnClickListener
                }

                ReservasRepository.actualizarReserva(
                    id = reservaId,
                    selecciones = selecciones
                ) { actualizada ->
                    if (actualizada == null) {
                        Toast.makeText(this, R.string.error_actualizar_reserva, Toast.LENGTH_LONG).show()
                        accionPrincipal.isEnabled = true
                        return@actualizarReserva
                    }

                    Toast.makeText(this, R.string.pedido_confirmado_exito, Toast.LENGTH_SHORT).show()
                    volverAMenuPrincipal()
                }
                return@setOnClickListener
            }

            if (fechaMillis <= 0L) {
                Toast.makeText(this, R.string.error_guardar_reserva, Toast.LENGTH_LONG).show()
                accionPrincipal.isEnabled = true
                return@setOnClickListener
            }

            ReservasRepository.agregarReserva(
                fechaMillis = fechaMillis,
                selecciones = selecciones
            ) { resultado ->
                when {
                    resultado.reservaCreada != null -> {
                        Toast.makeText(this, R.string.pedido_confirmado_exito, Toast.LENGTH_SHORT).show()
                        volverAMenuPrincipal()
                    }

                    resultado.reservaExistente != null -> {
                        Toast.makeText(this, R.string.reserva_ya_existente_en_fecha, Toast.LENGTH_LONG).show()
                        accionPrincipal.isEnabled = true
                    }

                    else -> {
                        Toast.makeText(this, R.string.error_guardar_reserva, Toast.LENGTH_LONG).show()
                        accionPrincipal.isEnabled = true
                    }
                }
            }
        }
    }

    private fun resolveRequest(): ConfirmacionReservaRequest? {
        val rawRequest = ConfirmacionReservaRawRequest(
            esEdicion = intent.getBooleanExtra(EXTRA_ES_EDICION, false),
            fecha = intent.getStringExtra(EXTRA_FECHA).orEmpty(),
            detalleSeleccion = intent.getStringExtra(EXTRA_DETALLE).orEmpty(),
            reservaId = intent.getStringExtra(EXTRA_RESERVA_ID).orEmpty(),
            fechaMillis = intent.getLongExtra(EXTRA_FECHA_MILLIS, -1L),
            hasSeleccionesPendientesExtra = intent.hasExtra(EXTRA_SELECCIONES_PENDIENTES),
            seleccionesPendientesRaw = run {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_SELECCIONES_PENDIENTES) as? HashMap<String, String>
            }.orEmpty()
        )

        return when (val resolution = resolveConfirmacionReservaRequest(rawRequest)) {
            is ConfirmacionReservaRequestResolution.Invalid -> invalidRequest(resolution.messageRes)
            is ConfirmacionReservaRequestResolution.Valid -> resolution.request
        }
    }

    private fun invalidRequest(@StringRes messageRes: Int): ConfirmacionReservaRequest? {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        return null
    }

    private fun applyBranding(
        titulo: TextView,
        detalle: TextView,
        fecha: TextView,
        principal: TextView,
        guarnicion: TextView,
        postre: TextView,
        accionPrincipal: Button,
        cards: List<MaterialCardView>
    ) {
        if (!AppRuntime.featureFlags.brandedConfirmationScreen) {
            return
        }

        val branding = AppRuntime.branding
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).setBackgroundResource(branding.confirmationBackgroundRes)

        val titleColor = ContextCompat.getColor(this, branding.confirmationTitleColorRes)
        val bodyColor = ContextCompat.getColor(this, branding.confirmationBodyTextColorRes)
        val cardColor = ContextCompat.getColor(this, branding.confirmationCardBackgroundColorRes)
        val strokeColor = ContextCompat.getColor(this, branding.confirmationCardStrokeColorRes)

        titulo.setTextColor(titleColor)
        detalle.setTextColor(bodyColor)
        fecha.setTextColor(titleColor)
        principal.setTextColor(titleColor)
        guarnicion.setTextColor(bodyColor)
        postre.setTextColor(bodyColor)

        cards.forEach { card ->
            card.setCardBackgroundColor(cardColor)
            card.strokeColor = strokeColor
        }

        accionPrincipal.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, branding.primaryActionColorRes)
        )
        accionPrincipal.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
        title = getString(branding.appNameRes)
    }


    private fun volverAMenuPrincipal() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun cargarImagenesMenuDesdeStorage(
        principalId: String?,
        guarnicionId: String?,
        postreId: String?,
        ivPrincipal: ImageView,
        ivGuarnicion: ImageView,
        ivPostre: ImageView
    ) {
        val imageUrlsByDish = MenuRepository.obtenerSecciones()
            .flatMap { it.opciones }
            .associate { plato -> plato.id to plato.imageUrl }

        imageByDishId = imageUrlsByDish

        if (imageByDishId.isEmpty()) {
            MenuRepository.cargarSecciones { ok, secciones ->
                if (!ok) return@cargarSecciones

                imageByDishId = secciones
                    .flatMap { it.opciones }
                    .associate { plato -> plato.id to plato.imageUrl }

                cargarImagenDesdeStorage(MenuIdentity.SECTION_MAIN, principalId, ivPrincipal)
                cargarImagenDesdeStorage(MenuIdentity.SECTION_SIDE, guarnicionId, ivGuarnicion)
                cargarImagenDesdeStorage(MenuIdentity.SECTION_DESSERT, postreId, ivPostre)
            }
            return
        }

        cargarImagenDesdeStorage(MenuIdentity.SECTION_MAIN, principalId, ivPrincipal)
        cargarImagenDesdeStorage(MenuIdentity.SECTION_SIDE, guarnicionId, ivGuarnicion)
        cargarImagenDesdeStorage(MenuIdentity.SECTION_DESSERT, postreId, ivPostre)
    }

    private fun cargarImagenDesdeStorage(sectionId: String, dishId: String?, imageView: ImageView) {
        val visibleName = MenuRepository.nombrePlato(sectionId, dishId)
        val fallbackImage = imageForSelection(visibleName)
        val normalizedDishId = dishId.orEmpty().trim()
        if (normalizedDishId.isBlank()) {
            imageView.setImageResource(fallbackImage)
            return
        }

        val imagePath = imageByDishId[normalizedDishId].orEmpty()
        if (imagePath.isBlank()) {
            imageView.setImageResource(fallbackImage)
            return
        }

        imageView.tag = normalizedDishId

        when {
            imagePath.startsWith("http", ignoreCase = true) -> {
                Glide.with(this)
                    .load(imagePath)
                    .placeholder(fallbackImage)
                    .error(fallbackImage)
                    .into(imageView)
            }

            imagePath.startsWith("gs://", ignoreCase = true) -> {
                storage.getReferenceFromUrl(imagePath).downloadUrl
                    .addOnSuccessListener { uri ->
                        aplicarImagenSiCorresponde(imageView, normalizedDishId, uri, fallbackImage)
                    }
                    .addOnFailureListener {
                        imageView.setImageResource(fallbackImage)
                    }
            }

            else -> {
                storage.reference.child(imagePath.trimStart('/')).downloadUrl
                    .addOnSuccessListener { uri ->
                        aplicarImagenSiCorresponde(imageView, normalizedDishId, uri, fallbackImage)
                    }
                    .addOnFailureListener {
                        imageView.setImageResource(fallbackImage)
                    }
            }
        }
    }

    private fun aplicarImagenSiCorresponde(
        imageView: ImageView,
        expectedTag: String,
        uri: Uri,
        fallbackImage: Int
    ) {
        if (imageView.tag != expectedTag) return

        Glide.with(this)
            .load(uri)
            .placeholder(fallbackImage)
            .error(fallbackImage)
            .into(imageView)
    }
}

internal data class ConfirmacionReservaRequest(
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

internal data class ConfirmacionReservaRawRequest(
    val esEdicion: Boolean,
    val fecha: String,
    val detalleSeleccion: String,
    val reservaId: String,
    val fechaMillis: Long,
    val hasSeleccionesPendientesExtra: Boolean,
    val seleccionesPendientesRaw: Map<String, String>
)

internal sealed interface ConfirmacionReservaRequestResolution {
    data class Valid(val request: ConfirmacionReservaRequest) : ConfirmacionReservaRequestResolution
    data class Invalid(@StringRes val messageRes: Int) : ConfirmacionReservaRequestResolution
}

internal fun resolveConfirmacionReservaRequest(
    rawRequest: ConfirmacionReservaRawRequest,
    sanitizeSelecciones: (Map<String, String>) -> Map<String, String> = ReservasRepository::sanitizeSelecciones,
    reservaById: (String) -> Reserva? = ReservasRepository::obtenerReservaPorId,
    canEditReservationDate: (Long) -> Boolean = ReservasRepository::puedeEditarReservaExistenteEnFecha,
    canCreateReservationDate: (Long) -> Boolean = ReservasRepository::puedeCrearReservaEnFecha
): ConfirmacionReservaRequestResolution {
    val seleccionesPendientes = sanitizeSelecciones(rawRequest.seleccionesPendientesRaw)

    if (rawRequest.hasSeleccionesPendientesExtra && seleccionesPendientes.isEmpty()) {
        return ConfirmacionReservaRequestResolution.Invalid(
            if (rawRequest.esEdicion) R.string.error_actualizar_reserva else R.string.error_guardar_reserva
        )
    }

    if (rawRequest.esEdicion) {
        val reserva = reservaById(rawRequest.reservaId)
        return if (rawRequest.reservaId.isBlank() || reserva == null || !canEditReservationDate(reserva.fechaMillis)) {
            ConfirmacionReservaRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
        } else {
            ConfirmacionReservaRequestResolution.Valid(
                ConfirmacionReservaRequest(
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
        ConfirmacionReservaRequestResolution.Invalid(R.string.error_detalle_reserva_no_disponible)
    } else {
        ConfirmacionReservaRequestResolution.Valid(
            ConfirmacionReservaRequest(
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
