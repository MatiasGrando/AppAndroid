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
import com.example.reservasapp.booking.BookingConfirmationPreview
import com.example.reservasapp.booking.BookingConfirmationRequest
import com.example.reservasapp.booking.BookingConfirmationRequestResolution
import com.example.reservasapp.booking.BookingConfirmationSubmitResult
import com.example.reservasapp.booking.BookingFlowService
import com.example.reservasapp.booking.BookingMenuCoordinator
import com.example.reservasapp.branding.AppRuntime
import com.example.reservasapp.firebase.FirebaseProvider
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla final del flujo: resume la seleccion y delega al servicio la confirmacion efectiva.
 */
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

        val esEdicion = request.esEdicion
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
        val cardGuarnicion = findViewById<MaterialCardView>(R.id.cardGuarnicion)
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
            cards = listOf(cardPrincipal, cardGuarnicion, cardPostre)
        )

        titulo.text = getString(
            if (esEdicion) R.string.titulo_confirmacion_edicion else R.string.titulo_confirmacion
        )

        val formatter = SimpleDateFormat("EEEE d/M/yy", Locale("es", "ES"))
        val fechaVisual = when {
            reserva != null -> Date(reserva.fechaMillis)
            request.fechaMillis > 0L -> Date(request.fechaMillis)
            else -> null
        }

        tvFechaReserva.text = fechaVisual?.let {
            formatter.format(it).uppercase(Locale("es", "ES"))
        } ?: request.fecha.uppercase(Locale("es", "ES"))

        if (reserva != null || request.seleccionesPendientes.isNotEmpty()) {
            detalle.visibility = View.GONE
        } else {
            detalle.visibility = View.VISIBLE
            detalle.text = getString(R.string.resumen_reserva_generico, request.fecha, request.detalleSeleccion)
        }

        val hayConfirmacionPendiente = request.seleccionesPendientes.isNotEmpty()
        accionPrincipal.text = getString(
            if (hayConfirmacionPendiente) R.string.confirmar_pedido else R.string.volver_menu
        )

        // La pantalla final ya habla con el coordinador de menu/preview y deja a BookingFlowService
        // como fachada de reglas del flujo, no de composicion visual.
        BookingMenuCoordinator.cargarVistaPreviaConfirmacion(request) { _, preview ->
            runOnUiThread {
                aplicarVistaPreviaConfirmacion(
                    preview = preview,
                    tvPrincipal = tvPrincipal,
                    tvGuarnicion = tvGuarnicion,
                    tvPostre = tvPostre,
                    cardGuarnicion = cardGuarnicion,
                    ivPrincipal = ivPrincipal,
                    ivGuarnicion = ivGuarnicion,
                    ivPostre = ivPostre
                )
            }
        }

        accionPrincipal.setOnClickListener {
            if (!hayConfirmacionPendiente) {
                volverAMenuPrincipal()
                return@setOnClickListener
            }

            accionPrincipal.isEnabled = false
            BookingFlowService.confirmarReserva(request) { result ->
                runOnUiThread {
                    when (result) {
                        BookingConfirmationSubmitResult.Success -> {
                            Toast.makeText(this, R.string.pedido_confirmado_exito, Toast.LENGTH_SHORT).show()
                            volverAMenuPrincipal()
                        }

                        BookingConfirmationSubmitResult.ExistingReservation -> {
                            Toast.makeText(this, R.string.reserva_ya_existente_en_fecha, Toast.LENGTH_LONG).show()
                            accionPrincipal.isEnabled = true
                        }

                        is BookingConfirmationSubmitResult.Error -> {
                            Toast.makeText(this, result.messageRes, Toast.LENGTH_LONG).show()
                            accionPrincipal.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Normaliza y valida los extras entrantes antes de tocar la UI o persistencia.
     */
    private fun resolveRequest(): BookingConfirmationRequest? {
        return when (val resolution = BookingFlowService.resolverConfirmacion(intent.toConfirmacionReservaRawRequest())) {
            is BookingConfirmationRequestResolution.Invalid -> invalidRequest(resolution.messageRes)
            is BookingConfirmationRequestResolution.Valid -> resolution.request
        }
    }

    private fun invalidRequest(@StringRes messageRes: Int): BookingConfirmationRequest? {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        return null
    }

    /**
     * Mapea la vista previa del servicio a la UI y mantiene los fallbacks visuales existentes.
     */
    private fun aplicarVistaPreviaConfirmacion(
        preview: BookingConfirmationPreview,
        tvPrincipal: TextView,
        tvGuarnicion: TextView,
        tvPostre: TextView,
        cardGuarnicion: View,
        ivPrincipal: ImageView,
        ivGuarnicion: ImageView,
        ivPostre: ImageView
    ) {
        imageByDishId = preview.imageUrlsByDishId

        val tieneGuarnicion = !preview.guarnicion.isNullOrBlank()
        tvPrincipal.text = preview.principal ?: "-"
        tvGuarnicion.text = preview.guarnicion.orEmpty()
        tvPostre.text = preview.postre ?: "-"
        cardGuarnicion.visibility = if (tieneGuarnicion) View.VISIBLE else View.GONE

        ivPrincipal.setImageResource(imageForSelection(preview.principal))
        ivGuarnicion.setImageResource(imageForSelection(preview.guarnicion))
        ivPostre.setImageResource(imageForSelection(preview.postre))

        cargarImagenDesdeStorage(preview.principalId, ivPrincipal, preview.principal)
        cargarImagenDesdeStorage(preview.guarnicionId, ivGuarnicion, preview.guarnicion)
        cargarImagenDesdeStorage(preview.postreId, ivPostre, preview.postre)
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

    /**
     * Usa el mapa de imagenes resuelto por el servicio y cae al drawable local si falta URL.
     */
    private fun cargarImagenDesdeStorage(dishId: String?, imageView: ImageView, visibleName: String?) {
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
