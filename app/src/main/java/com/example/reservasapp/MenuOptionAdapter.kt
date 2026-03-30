package com.example.reservasapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.content.res.ColorStateList
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class MenuOptionAdapter(
    private var items: List<MenuItemOption>,
    private val onSelected: (MenuItemOption, Boolean) -> Unit
) : RecyclerView.Adapter<MenuOptionAdapter.MenuOptionViewHolder>() {

    private var lastTappedPosition = RecyclerView.NO_POSITION
    private var lastTapTimestamp = 0L

    private var selectedPosition = RecyclerView.NO_POSITION
    private var themePalette = MenuThemeRegistry.palette()

    fun updateItems(newItems: List<MenuItemOption>, selectedId: String?) {
        items = newItems
        selectedPosition = selectedId?.let { id ->
            items.indexOfFirst { it.id == id }
        }?.takeIf { it >= 0 } ?: RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun updateTheme(palette: MenuThemePalette) {
        themePalette = palette
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuOptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_menu_option, parent, false)
        return MenuOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuOptionViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position == selectedPosition, themePalette)
        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

            val now = System.currentTimeMillis()
            val isDoubleTap = lastTappedPosition == currentPosition && now - lastTapTimestamp <= DOUBLE_TAP_WINDOW_MS
            lastTappedPosition = currentPosition
            lastTapTimestamp = now

            val prev = selectedPosition
            selectedPosition = currentPosition
            if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev)
            notifyItemChanged(selectedPosition)
            onSelected(item, isDoubleTap)
        }
    }

    override fun getItemCount(): Int = items.size

    class MenuOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card = itemView.findViewById<MaterialCardView>(R.id.cardMenu)
        private val image = itemView.findViewById<ShapeableImageView>(R.id.ivMenu)
        private val title = itemView.findViewById<TextView>(R.id.tvMenuTitle)
        private val description = itemView.findViewById<TextView>(R.id.tvMenuDescription)

        fun bind(item: MenuItemOption, isSelected: Boolean, palette: MenuThemePalette) {
            val fallbackImage = ComidaImageRepository.obtenerImagenComida(item.name)
            if (item.imageUrl.isNotBlank()) {
                Glide.with(itemView)
                    .load(item.imageUrl)
                    .placeholder(fallbackImage)
                    .error(fallbackImage)
                    .into(image)
            } else {
                image.setImageResource(fallbackImage)
            }
            title.text = item.name
            description.text = item.description
            title.setTextColor(palette.optionTitleColor)
            description.setTextColor(palette.optionDescriptionColor)
            image.setBackgroundColor(palette.imageBackgroundColor)
            image.strokeColor = ColorStateList.valueOf(palette.imageStrokeColor)

            card.setCardBackgroundColor(if (isSelected) palette.optionCardSelectedColor else palette.optionCardDefaultColor)
            card.strokeColor = if (isSelected) palette.optionCardSelectedStrokeColor else palette.optionCardDefaultStrokeColor
            card.strokeWidth = if (isSelected) 3 else 1
        }
    }

    companion object {
        private const val DOUBLE_TAP_WINDOW_MS = 400L
    }
}
