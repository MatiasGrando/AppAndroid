package com.example.reservasapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView

class MenuOptionAdapter(
    private var items: List<MenuItemOption>,
    private val onSelected: (MenuItemOption) -> Unit
) : RecyclerView.Adapter<MenuOptionAdapter.MenuOptionViewHolder>() {

    private var selectedPosition = 0

    fun updateItems(newItems: List<MenuItemOption>) {
        items = newItems
        selectedPosition = 0
        notifyDataSetChanged()
        items.firstOrNull()?.let { onSelected(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_option, parent, false)
        return MenuOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuOptionViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == selectedPosition)
        holder.itemView.setOnClickListener {
            val prev = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class MenuOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.cardMenu)
        private val image = itemView.findViewById<ImageView>(R.id.ivMenu)
        private val title = itemView.findViewById<TextView>(R.id.tvMenuTitle)
        private val description = itemView.findViewById<TextView>(R.id.tvMenuDescription)
        private val price = itemView.findViewById<TextView>(R.id.tvPrice)

        fun bind(item: MenuItemOption, isSelected: Boolean) {
            image.setImageResource(item.imageRes)
            title.text = item.name
            description.text = item.description
            price.text = item.price

            card.setCardBackgroundColor(if (isSelected) Color.parseColor("#F0FFFA") else Color.WHITE)
            card.strokeColor = if (isSelected) Color.parseColor("#25B78C") else Color.parseColor("#E3E3E3")
            card.strokeWidth = if (isSelected) 3 else 1
        }
    }
}
