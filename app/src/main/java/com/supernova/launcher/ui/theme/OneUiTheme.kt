package com.supernova.launcher.ui.theme

import android.content.Context
import android.graphics.Color
import com.supernova.launcher.R
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.data.SettingEnumSerializer

@kotlinx.serialization.Serializable(with = DarkStyle.Names::class)
enum class DarkStyle {
    DEFAULT,
    AMOLED;

    object Names : SettingEnumSerializer<DarkStyle>("DarkStyle", entries, DEFAULT)

    companion object {
        fun fromName(name: String): DarkStyle = Names.parse(name)
    }
}

@kotlinx.serialization.Serializable(with = LightStyle.Names::class)
enum class LightStyle {
    DEFAULT,
    FROSTED;

    object Names : SettingEnumSerializer<LightStyle>("LightStyle", entries, DEFAULT)

    companion object {
        fun fromName(name: String): LightStyle = Names.parse(name)
    }
}

enum class ThemeStyleVariant(val storageKey: String) {
    DARK_DEFAULT("dark_default"),
    DARK_AMOLED("dark_amoled"),
    LIGHT_DEFAULT("light_default"),
    LIGHT_FROSTED("light_frosted");

    val isDark: Boolean get() = this == DARK_DEFAULT || this == DARK_AMOLED
}

object OneUiTheme {

    // Pure black and OneUI-inspired dark tones
    const val AMOLED_BLACK = 0xFF000000.toInt()
    const val AMOLED_CARD = 0xFF0A0A0A.toInt()
    const val AMOLED_BAR = 0xFF141414.toInt()
    const val AMOLED_POPUP = 0xFF0D0D0D.toInt()
    const val AMOLED_FOLDER_PLATE = 0x66000000

    // Frosted light tones
    const val FROSTED_WHITE = 0xFFFFFFFF.toInt()
    const val FROSTED_SURFACE = 0xF2FFFFFF.toInt()
    const val FROSTED_CARD = 0xE6F8F9FA.toInt()
    const val FROSTED_BAR = 0xCCFFFFFF.toInt()
    const val FROSTED_FOLDER_PLATE = 0x80FFFFFF.toInt()

    fun activeVariant(context: Context, prefs: LauncherPrefs): ThemeStyleVariant {
        val night = ThemeEngine.isNight(context, prefs)
        return when {
            night && prefs.darkStyle == DarkStyle.AMOLED -> ThemeStyleVariant.DARK_AMOLED
            night -> ThemeStyleVariant.DARK_DEFAULT
            prefs.lightStyle == LightStyle.FROSTED -> ThemeStyleVariant.LIGHT_FROSTED
            else -> ThemeStyleVariant.LIGHT_DEFAULT
        }
    }

    fun defaultSurfaceColor(
        variant: ThemeStyleVariant,
        surfaceId: SurfaceId,
        fallbackColor: Int
    ): Int = when (variant) {
        ThemeStyleVariant.DARK_AMOLED -> when (surfaceId) {
            SurfaceId.DRAWER, SurfaceId.SETTINGS, SurfaceId.HOME -> AMOLED_BLACK
            SurfaceId.FOLDER -> AMOLED_BLACK
            SurfaceId.CARDS, SurfaceId.POPUP -> AMOLED_CARD
            SurfaceId.SEARCH_BAR, SurfaceId.DRAWER_SEARCH_BAR -> AMOLED_BAR
            SurfaceId.DOCK -> AMOLED_BLACK
        }
        ThemeStyleVariant.LIGHT_FROSTED -> when (surfaceId) {
            SurfaceId.FOLDER -> FROSTED_SURFACE
            SurfaceId.CARDS, SurfaceId.POPUP -> FROSTED_CARD
            SurfaceId.SEARCH_BAR, SurfaceId.DRAWER_SEARCH_BAR -> FROSTED_BAR
            SurfaceId.DRAWER -> FROSTED_SURFACE
            SurfaceId.DOCK -> (0xCC shl 24) or (fallbackColor and 0x00FFFFFF)
            else -> fallbackColor
        }
        ThemeStyleVariant.DARK_DEFAULT -> when (surfaceId) {
            // Fix dark theme search bar defaulting to light when system context was light
            SurfaceId.SEARCH_BAR -> 0xFF1A2230.toInt()
            SurfaceId.DRAWER_SEARCH_BAR -> 0x24FFFFFF
            else -> fallbackColor
        }
        ThemeStyleVariant.LIGHT_DEFAULT -> fallbackColor
    }

    fun defaultFolderPlateColor(variant: ThemeStyleVariant): Int = when (variant) {
        ThemeStyleVariant.DARK_AMOLED -> AMOLED_FOLDER_PLATE
        ThemeStyleVariant.DARK_DEFAULT -> 0x59000000
        ThemeStyleVariant.LIGHT_FROSTED -> FROSTED_FOLDER_PLATE
        ThemeStyleVariant.LIGHT_DEFAULT -> 0x59FFFFFF
    }
}
