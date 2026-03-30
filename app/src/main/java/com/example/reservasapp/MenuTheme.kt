package com.example.reservasapp

import android.graphics.Color
import androidx.core.content.ContextCompat

data class MenuThemePalette(
    val backgroundDrawableRes: Int,
    val panelBackgroundColor: Int,
    val titleColor: Int,
    val bodyTextColor: Int,
    val tabSelectedColor: Int,
    val tabUnselectedColor: Int,
    val tabIndicatorColor: Int,
    val hintTextColor: Int,
    val buttonBackgroundColor: Int,
    val buttonTextColor: Int,
    val optionCardSelectedColor: Int,
    val optionCardDefaultColor: Int,
    val optionCardSelectedStrokeColor: Int,
    val optionCardDefaultStrokeColor: Int,
    val optionTitleColor: Int,
    val optionDescriptionColor: Int,
    val imageStrokeColor: Int,
    val imageBackgroundColor: Int
)

object MenuThemeRegistry {
    private val buttonBackgroundColor: Int
        get() = ContextCompat.getColor(ReservasApp.instance, R.color.button_primary_fill)

    private val buttonTextColor: Int
        get() = ContextCompat.getColor(ReservasApp.instance, R.color.button_primary_text)

    fun palette(): MenuThemePalette {
        return MenuThemePalette(
            backgroundDrawableRes = R.drawable.bg_asian_food_dark,
            panelBackgroundColor = Color.parseColor("#D9101A24"),
            titleColor = Color.parseColor("#F6E9C5"),
            bodyTextColor = Color.parseColor("#D9D2C1"),
            tabSelectedColor = Color.parseColor("#F1DDAD"),
            tabUnselectedColor = Color.parseColor("#B7BFD1"),
            tabIndicatorColor = Color.parseColor("#F1DDAD"),
            hintTextColor = Color.parseColor("#F1DDAD"),
            buttonBackgroundColor = buttonBackgroundColor,
            buttonTextColor = buttonTextColor,
            optionCardSelectedColor = Color.parseColor("#1E3142"),
            optionCardDefaultColor = Color.parseColor("#D9101A24"),
            optionCardSelectedStrokeColor = Color.parseColor("#F1DDAD"),
            optionCardDefaultStrokeColor = Color.parseColor("#37506A"),
            optionTitleColor = Color.parseColor("#F6E9C5"),
            optionDescriptionColor = Color.parseColor("#D2D9E4"),
            imageStrokeColor = Color.parseColor("#C99A61"),
            imageBackgroundColor = Color.parseColor("#1E2D3B")
        )
    }
}
