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

    // Folders, search bars and surfaces
    const val FROSTED_WHITE = 0xFFFFFFFF.toInt()
    const val FROSTED_SURFACE = 0xF2FFFFFF.toInt()
    const val FROSTED_CARD = 0xE6F8F9FA.toInt()
    const val FROSTED_BAR = 0xCCFFFFFF.toInt()
    const val FROSTED_FOLDER_PLATE = 0x80FFFFFF.toInt()

    private const val DARK_DEFAULT_SURFACE = 0xFF1E2735.toInt()
    private const val DARK_DEFAULT_CARD = 0xFF1A2230.toInt()
    private const val DARK_DEFAULT_SETTINGS = 0xFF0D1117.toInt()
    private const val DARK_DEFAULT_DRAWER_SEARCH_BAR = 0x24FFFFFF

    private const val LIGHT_FROSTED_SETTINGS = 0xFFF5F7FA.toInt()
    private const val LIGHT_FROSTED_DOCK = 0xCCFFFFFF.toInt()

    private const val LIGHT_DEFAULT_SETTINGS = 0xFFF4F5F7.toInt()

    private const val PLATE_AMOLED = 0x99000000.toInt()
    private const val PLATE_DARK_DEFAULT = 0x66000000.toInt()
    private const val PLATE_LIGHT_FROSTED = 0x99FFFFFF.toInt()
    private const val PLATE_LIGHT_DEFAULT = 0x80FFFFFF.toInt()

    fun activeVariant(context: Context, prefs: LauncherPrefs): ThemeStyleVariant {
        val night = ThemeEngine.isNight(context, prefs)
        return when {
            night && prefs.darkStyle == DarkStyle.AMOLED -> ThemeStyleVariant.DARK_AMOLED
            night -> ThemeStyleVariant.DARK_DEFAULT
            prefs.lightStyle == LightStyle.FROSTED -> ThemeStyleVariant.LIGHT_FROSTED
            else -> ThemeStyleVariant.LIGHT_DEFAULT
        }
    }

    private fun surfaceForDarkAmoled(surfaceId: SurfaceId): Int = when (surfaceId) {
        SurfaceId.DRAWER, SurfaceId.SETTINGS, SurfaceId.HOME, SurfaceId.FOLDER, SurfaceId.DOCK -> AMOLED_BLACK
        SurfaceId.CARDS, SurfaceId.POPUP -> AMOLED_CARD
        SurfaceId.SEARCH_BAR, SurfaceId.DRAWER_SEARCH_BAR -> AMOLED_BAR
    }

    private fun surfaceForDarkDefault(surfaceId: SurfaceId, fallbackColor: Int): Int = when (surfaceId) {
        SurfaceId.FOLDER, SurfaceId.DRAWER -> DARK_DEFAULT_SURFACE
        SurfaceId.CARDS, SurfaceId.POPUP, SurfaceId.SEARCH_BAR -> DARK_DEFAULT_CARD
        SurfaceId.DRAWER_SEARCH_BAR -> DARK_DEFAULT_DRAWER_SEARCH_BAR
        SurfaceId.SETTINGS -> DARK_DEFAULT_SETTINGS
        else -> fallbackColor
    }

    private fun surfaceForLightFrosted(surfaceId: SurfaceId, fallbackColor: Int): Int = when (surfaceId) {
        SurfaceId.FOLDER, SurfaceId.DRAWER -> FROSTED_SURFACE
        SurfaceId.CARDS, SurfaceId.POPUP -> FROSTED_CARD
        SurfaceId.SEARCH_BAR, SurfaceId.DRAWER_SEARCH_BAR -> FROSTED_BAR
        SurfaceId.SETTINGS -> LIGHT_FROSTED_SETTINGS
        SurfaceId.DOCK -> LIGHT_FROSTED_DOCK
        else -> fallbackColor
    }

    private fun surfaceForLightDefault(surfaceId: SurfaceId, fallbackColor: Int): Int = when (surfaceId) {
        SurfaceId.FOLDER, SurfaceId.DRAWER, SurfaceId.CARDS,
        SurfaceId.POPUP, SurfaceId.SEARCH_BAR, SurfaceId.DRAWER_SEARCH_BAR -> FROSTED_WHITE
        SurfaceId.SETTINGS -> LIGHT_DEFAULT_SETTINGS
        else -> fallbackColor
    }

    fun defaultSurfaceColor(
        variant: ThemeStyleVariant,
        surfaceId: SurfaceId,
        fallbackColor: Int
    ): Int = when (variant) {
        ThemeStyleVariant.DARK_AMOLED -> surfaceForDarkAmoled(surfaceId)
        ThemeStyleVariant.DARK_DEFAULT -> surfaceForDarkDefault(surfaceId, fallbackColor)
        ThemeStyleVariant.LIGHT_FROSTED -> surfaceForLightFrosted(surfaceId, fallbackColor)
        ThemeStyleVariant.LIGHT_DEFAULT -> surfaceForLightDefault(surfaceId, fallbackColor)
    }

    fun defaultFolderPlateColor(variant: ThemeStyleVariant): Int = when (variant) {
        ThemeStyleVariant.DARK_AMOLED -> PLATE_AMOLED
        ThemeStyleVariant.DARK_DEFAULT -> PLATE_DARK_DEFAULT
        ThemeStyleVariant.LIGHT_FROSTED -> PLATE_LIGHT_FROSTED
        ThemeStyleVariant.LIGHT_DEFAULT -> PLATE_LIGHT_DEFAULT
    }
}
