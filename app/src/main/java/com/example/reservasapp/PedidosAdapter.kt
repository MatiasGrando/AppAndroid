package com.example.reservasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PedidosAdapter(
    private var pedidos: List<Pedido>,
    private val esAdmin: Boolean,
    private val onEstadoChange: (Pedido, String) -> Unit
) : RecyclerView.Adapter<PedidosAdapter.PedidoViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "ES"))

    fun actualizarPedidos(nuevosPedidos: List<Pedido>) {
        pedidos = nuevosPedidos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun getItemCount(): Int = pedidos.size

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(pedidos[position], esAdmin, onEstadoChange, dateFormatter)
    }

    class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre = itemView.findViewById<TextView>(R.id.tvNombreUsuarioPedido)
        private val tvFecha = itemView.findViewById<TextView>(R.id.tvFechaPedido)
        private val tvItems = itemView.findViewById<TextView>(R.id.tvItemsPedido)
        private val tvTotal = itemView.findViewById<TextView>(R.id.tvTotalPedido)
        private val spinnerEstado = itemView.findViewById<Spinner>(R.id.spinnerEstadoPedido)

        fun bind(
            pedido: Pedido,
            esAdmin: Boolean,
            onEstadoChange: (Pedido, String) -> Unit,
            formatter: SimpleDateFormat
        ) {
            tvNombre.text = pedido.nombreUsuario
            tvFecha.text = formatter.format(Date(pedido.fecha))
            tvItems.text = pedido.items.joinToString("\n")
            tvTotal.text = itemView.context.getString(R.string.total_formato, pedido.total)

            val adapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                Pedido.estadosValidos
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerEstado.adapter = adapter
            spinnerEstado.setSelection(Pedido.estadosValidos.indexOf(pedido.estadoPedido).coerceAtLeast(0), false)
            spinnerEstado.isEnabled = esAdmin
            if (esAdmin) {
                spinnerEstado.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val nuevoEstado = Pedido.estadosValidos[position]
                        if (nuevoEstado != pedido.estadoPedido) {
                            onEstadoChange(pedido, nuevoEstado)
                        }
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                })
            }
        }
    }
}
