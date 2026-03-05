package com.example.reservasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MisReservasAdapter(
    private var reservas: List<Reserva>
) : RecyclerView.Adapter<MisReservasAdapter.MisReservasViewHolder>() {

    private val dateFormatter = SimpleDateFormat("EEEE d/M/yy", Locale("es", "ES"))

    fun updateData(newReservas: List<Reserva>) {
        reservas = newReservas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisReservasViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mis_reserva, parent, false)
        return MisReservasViewHolder(view)
    }

    override fun onBindViewHolder(holder: MisReservasViewHolder, position: Int) {
        holder.bind(reservas[position], dateFormatter)
    }

    override fun getItemCount(): Int = reservas.size

    class MisReservasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFecha = itemView.findViewById<TextView>(R.id.tvFechaReserva)
        private val tvPrincipalNombre = itemView.findViewById<TextView>(R.id.tvPlatoPrincipalNombre)
        private val ivPrincipal = itemView.findViewById<ImageView>(R.id.ivPlatoPrincipal)
        private val tvGuarnicionNombre = itemView.findViewById<TextView>(R.id.tvGuarnicionNombre)
        private val ivGuarnicion = itemView.findViewById<ImageView>(R.id.ivGuarnicion)
        private val tvPostreNombre = itemView.findViewById<TextView>(R.id.tvPostreNombre)
        private val ivPostre = itemView.findViewById<ImageView>(R.id.ivPostre)

        fun bind(reserva: Reserva, formatter: SimpleDateFormat) {
            val principal = extraerSeleccion(reserva.selecciones, "plato", "principal")
            val guarnicion = extraerSeleccion(reserva.selecciones, "guarn")
            val postre = extraerSeleccion(reserva.selecciones, "postre")

            tvFecha.text = formatter.format(Date(reserva.fechaMillis)).uppercase(Locale("es", "ES"))

            tvPrincipalNombre.text = principal ?: "-"
            tvGuarnicionNombre.text = guarnicion ?: "-"
            tvPostreNombre.text = postre ?: "-"

            ivPrincipal.setImageResource(imageForSelection(principal))
            ivGuarnicion.setImageResource(imageForSelection(guarnicion))
            ivPostre.setImageResource(imageForSelection(postre))
        }

        private fun extraerSeleccion(selecciones: Map<String, String>, vararg aliases: String): String? {
            return selecciones.entries.firstOrNull { (key, _) ->
                val keyNormalized = key.lowercase()
                aliases.all { keyNormalized.contains(it.lowercase()) }
            }?.value
        }
    }
}
