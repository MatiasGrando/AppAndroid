package com.example.reservasapp

import android.graphics.Color
import androidx.core.content.ContextCompat

enum class MenuVisualTheme(val storageValue: String) {
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun fromStorage(value: String?): MenuVisualTheme {
            return entries.firstOrNull { it.storageValue == value } ?: DARK
        }
    }
}

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

    fun palette(theme: MenuVisualTheme): MenuThemePalette {
        return when (theme) {
            MenuVisualTheme.DARK -> MenuThemePalette(
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

            MenuVisualTheme.LIGHT -> MenuThemePalette(
                backgroundDrawableRes = R.drawable.bg_asian_food_light,
                panelBackgroundColor = Color.parseColor("#EBF5EFE7"),
                titleColor = Color.parseColor("#3A2A1D"),
                bodyTextColor = Color.parseColor("#5A4A3A"),
                tabSelectedColor = Color.parseColor("#7A613F"),
                tabUnselectedColor = Color.parseColor("#7E7465"),
                tabIndicatorColor = Color.parseColor("#B08D57"),
                hintTextColor = Color.parseColor("#4A3928"),
                buttonBackgroundColor = buttonBackgroundColor,
                buttonTextColor = buttonTextColor,
                optionCardSelectedColor = Color.parseColor("#FFF7E8"),
                optionCardDefaultColor = Color.parseColor("#F4EDE2"),
                optionCardSelectedStrokeColor = Color.parseColor("#B08D57"),
                optionCardDefaultStrokeColor = Color.parseColor("#D6C5AA"),
                optionTitleColor = Color.parseColor("#6A5536"),
                optionDescriptionColor = Color.parseColor("#4B4031"),
                imageStrokeColor = Color.parseColor("#B08D57"),
                imageBackgroundColor = Color.parseColor("#F5E6D2")
            )
        }
    }
}
