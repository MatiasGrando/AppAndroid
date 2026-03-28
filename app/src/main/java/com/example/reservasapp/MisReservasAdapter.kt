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
    private val imageByDishId = imageUrlsByDish
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
        return MisReservasViewHolder(view, imageByDishId)
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
        private val imageByDishId: Map<String, String>
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById<TextView>(R.id.tvFechaReserva)
        private val tvPrincipalNombre = itemView.findViewById<TextView>(R.id.tvPlatoPrincipalNombre)
        private val ivPrincipal = itemView.findViewById<ImageView>(R.id.ivPlatoPrincipal)
        private val rowGuarnicion = itemView.findViewById<View>(R.id.rowGuarnicion)
        private val dividerPostGuarnicion = itemView.findViewById<View>(R.id.dividerPostGuarnicion)
        private val tvGuarnicionNombre = itemView.findViewById<TextView>(R.id.tvGuarnicionNombre)
        private val ivGuarnicion = itemView.findViewById<ImageView>(R.id.ivGuarnicion)
        private val tvPostreNombre = itemView.findViewById<TextView>(R.id.tvPostreNombre)
        private val ivPostre = itemView.findViewById<ImageView>(R.id.ivPostre)
        private val storage by lazy { FirebaseStorage.getInstance() }

        fun bind(reserva: Reserva, formatter: SimpleDateFormat, isSelected: Boolean) {
            val principalId = reserva.selecciones[MenuIdentity.SECTION_MAIN]
            val guarnicionId = reserva.selecciones[MenuIdentity.SECTION_SIDE]
            val postreId = reserva.selecciones[MenuIdentity.SECTION_DESSERT]
            val principal = MenuRepository.nombrePlato(MenuIdentity.SECTION_MAIN, principalId)
            val guarnicion = MenuRepository.nombrePlato(MenuIdentity.SECTION_SIDE, guarnicionId)
            val postre = MenuRepository.nombrePlato(MenuIdentity.SECTION_DESSERT, postreId)

            tvFecha.text = formatter.format(Date(reserva.fechaMillis)).uppercase(Locale("es", "ES"))
            itemView.alpha = if (isSelected) 1f else 0.9f

            val tieneGuarnicion = !guarnicion.isNullOrBlank() && guarnicion != "-"

            tvPrincipalNombre.text = principal ?: "-"
            tvGuarnicionNombre.text = guarnicion.orEmpty()
            tvPostreNombre.text = postre ?: "-"
            rowGuarnicion.visibility = if (tieneGuarnicion) View.VISIBLE else View.GONE
            dividerPostGuarnicion.visibility = if (tieneGuarnicion) View.VISIBLE else View.GONE

            cargarImagenDesdeStorage(MenuIdentity.SECTION_MAIN, principalId, ivPrincipal)
            if (tieneGuarnicion) {
                cargarImagenDesdeStorage(MenuIdentity.SECTION_SIDE, guarnicionId, ivGuarnicion)
            }
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
                    Glide.with(itemView)
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

            Glide.with(itemView)
                .load(uri)
                .placeholder(fallbackImage)
                .error(fallbackImage)
                .into(imageView)
        }

    }
}
