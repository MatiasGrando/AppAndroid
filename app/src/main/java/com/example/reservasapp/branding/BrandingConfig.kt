package com.example.reservasapp.branding

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class BrandingConfig(
    @StringRes val appNameRes: Int,
    @StringRes val loginTitleRes: Int,
    @StringRes val homeTitleRes: Int,
    @StringRes val homeSubtitleRes: Int,
    @StringRes val homePrimaryActionRes: Int,
    @StringRes val homeSecondaryActionRes: Int,
    @DrawableRes val appLogoRes: Int,
    @DrawableRes val homeHeroLogoRes: Int,
    @DrawableRes val loginDecorRes: Int,
    @DrawableRes val homeBackgroundRes: Int,
    @DrawableRes val confirmationBackgroundRes: Int,
    @ColorRes val loginBackgroundColorRes: Int,
    @ColorRes val loginOverlayColorRes: Int,
    @ColorRes val homeOverlayColorRes: Int,
    @ColorRes val primaryActionColorRes: Int,
    @ColorRes val secondaryActionColorRes: Int,
    @ColorRes val actionTextColorRes: Int,
    @ColorRes val confirmationTitleColorRes: Int,
    @ColorRes val confirmationBodyTextColorRes: Int,
    @ColorRes val confirmationCardBackgroundColorRes: Int,
    @ColorRes val confirmationCardStrokeColorRes: Int
)
