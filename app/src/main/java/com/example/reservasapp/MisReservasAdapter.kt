package com.example.reservasapp

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MisReservasAdapter(
    private var reservas: List<Reserva>,
    imageUrlsByDish: Map<String, String>,
    private val onReservaSelected: (Reserva?) -> Unit
) : RecyclerView.Adapter<MisReservasAdapter.MisReservasViewHolder>() {

    private val dateFormatter = SimpleDateFormat("EEEE d/M/yy", Locale("es", "ES"))
    private val imageByDishNormalized = imageUrlsByDish
        .mapKeys { (dishName, _) -> normalizarNombre(dishName) }
    private var selectedPosition: Int = RecyclerView.NO_POSITION

    fun updateData(newReservas: List<Reserva>) {
        reservas = newReservas
        selectedPosition = RecyclerView.NO_POSITION
        onReservaSelected(null)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisReservasViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mis_reserva, parent, false)
        return MisReservasViewHolder(view, imageByDishNormalized)
    }

    override fun onBindViewHolder(holder: MisReservasViewHolder, position: Int) {
        holder.bind(reservas[position], dateFormatter, position == selectedPosition)
        holder.itemView.setOnClickListener {
            val previousSelection = selectedPosition
            selectedPosition = if (selectedPosition == position) RecyclerView.NO_POSITION else position

            if (previousSelection != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelection)
            }
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition)
                onReservaSelected(reservas[selectedPosition])
            } else {
                onReservaSelected(null)
            }
        }
    }

    override fun getItemCount(): Int = reservas.size

    class MisReservasViewHolder(
        itemView: View,
        private val imageByDishNormalized: Map<String, String>
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById<TextView>(R.id.tvFechaReserva)
        private val tvPrincipalNombre = itemView.findViewById<TextView>(R.id.tvPlatoPrincipalNombre)
        private val ivPrincipal = itemView.findViewById<ImageView>(R.id.ivPlatoPrincipal)
        private val tvGuarnicionNombre = itemView.findViewById<TextView>(R.id.tvGuarnicionNombre)
        private val ivGuarnicion = itemView.findViewById<ImageView>(R.id.ivGuarnicion)
        private val tvPostreNombre = itemView.findViewById<TextView>(R.id.tvPostreNombre)
        private val ivPostre = itemView.findViewById<ImageView>(R.id.ivPostre)
        private val storage by lazy { FirebaseStorage.getInstance() }

        fun bind(reserva: Reserva, formatter: SimpleDateFormat, isSelected: Boolean) {
            val principal = extraerSeleccion(reserva.selecciones, "plato", "principal")
            val guarnicion = extraerSeleccion(reserva.selecciones, "guarn")
            val postre = extraerSeleccion(reserva.selecciones, "postre")

            tvFecha.text = formatter.format(Date(reserva.fechaMillis)).uppercase(Locale("es", "ES"))
            itemView.alpha = if (isSelected) 1f else 0.9f

            tvPrincipalNombre.text = principal ?: "-"
            tvGuarnicionNombre.text = guarnicion ?: "-"
            tvPostreNombre.text = postre ?: "-"

            cargarImagenDesdeStorage(principal, ivPrincipal)
            cargarImagenDesdeStorage(guarnicion, ivGuarnicion)
            cargarImagenDesdeStorage(postre, ivPostre)
        }

        private fun cargarImagenDesdeStorage(nombrePlato: String?, imageView: ImageView) {
            val fallbackImage = imageForSelection(nombrePlato)
            val nombreNormalizado = normalizarNombre(nombrePlato.orEmpty())
            if (nombreNormalizado.isBlank()) {
                imageView.setImageResource(fallbackImage)
                return
            }

            val imagePath = imageByDishNormalized[nombreNormalizado].orEmpty()
            if (imagePath.isBlank()) {
                imageView.setImageResource(fallbackImage)
                return
            }

            imageView.tag = nombreNormalizado

            when {
                imagePath.startsWith("http", ignoreCase = true) -> {
                    Glide.with(itemView)
                        .load(imagePath)
                        .placeholder(fallbackImage)
                        .error(fallbackImage)
                        .into(imageView)
                }

                imagePath.startsWith("gs://", ignoreCase = true) -> {
                    storage.getReferenceFromUrl(imagePath).downloadUrl
                        .addOnSuccessListener { uri ->
                            aplicarImagenSiCorresponde(imageView, nombreNormalizado, uri, fallbackImage)
                        }
                        .addOnFailureListener {
                            imageView.setImageResource(fallbackImage)
                        }
                }

                else -> {
                    storage.reference.child(imagePath.trimStart('/')).downloadUrl
                        .addOnSuccessListener { uri ->
                            aplicarImagenSiCorresponde(imageView, nombreNormalizado, uri, fallbackImage)
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

            Glide.with(itemView)
                .load(uri)
                .placeholder(fallbackImage)
                .error(fallbackImage)
                .into(imageView)
        }

        private fun extraerSeleccion(selecciones: Map<String, String>, vararg aliases: String): String? {
            return selecciones.entries.firstOrNull { (key, _) ->
                val keyNormalized = key.lowercase()
                aliases.all { keyNormalized.contains(it.lowercase()) }
            }?.value
        }
    }
}

private fun normalizarNombre(nombre: String): String {
    return nombre
        .trim()
        .lowercase(Locale.ROOT)
        .replace("\\s+".toRegex(), " ")
}
