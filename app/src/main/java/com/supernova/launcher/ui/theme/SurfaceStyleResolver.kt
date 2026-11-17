package com.supernova.launcher.ui.theme

import android.content.Context
import android.content.res.Configuration
import com.supernova.launcher.R
import com.supernova.launcher.data.LauncherPrefs
import com.supernova.launcher.ui.LabelStyler

// Final ints for one surface; the surface color carries its effective alpha.
class ResolvedSurface(
    val surface: Int,
    val text: Int,
    val accent: Int,
    // [topLeft, topRight, bottomRight, bottomLeft] in dp, or null for the
    // surface's built-in shape.
    val cornerRadiiDp: FloatArray?
)

class ResolvedStyles(private val surfaces: Map<SurfaceId, ResolvedSurface>) {
    fun of(id: SurfaceId): ResolvedSurface = surfaces.getValue(id)
}

// Memoized (config, palette, night) -> ints resolution. Everything renders
// from plain ints through the existing guarded chrome paths, so resolution
// cost is paid once per config/palette/theme change, never per frame.
object Styles {

    // Bumped by the wallpaper-colors listener; forces re-resolution.
    @Volatile
    private var wallpaperGeneration = 0

    fun onWallpaperChanged() {
        wallpaperGeneration++
    }

    private data class Key(
        val config: String,
        val source: String,
        val seed: String,
        val night: Boolean,
        val accent: String,
        val drawerBgColor: String,
        val folderBgColor: String,
        val drawerAlpha: Float,
        val dockAlpha: Float,
        val generation: Int,
        val variant: String = ""
    )

    private var cachedKey: Key? = null
    private var cachedStyles: ResolvedStyles? = null
    private var cachedPaletteKey: Key? = null
    private var cachedPalette: TonalPalette? = null

    const val DOCK_BASE_RGB = 0x1A2230
    const val DRAWER_FIELD_ALPHA = 0x24
    // Below this opacity (above 70% transparency) the drawer's labels and
    // text default to the wallpaper's light ones.
    const val SEE_THROUGH_ALPHA = 0.3f
    // The light theme's textPrimary, the dark text a light drawer gets in
    // either theme.
    const val DARK_TEXT = 0xFF11161D.toInt()

    fun isSeeThrough(alpha: Float): Boolean = alpha < SEE_THROUGH_ALPHA

    // The drawer's text with no text color set: read off the drawer's own
    // color, dark on a light one and light on a dark one whatever the
    // theme, or the wallpaper's (`onWallpaper`) when the drawer barely
    // covers it. Before, it followed the theme alone, so a dark drawer
    // color in light mode drew dark labels on it.
    fun drawerText(surfaceBase: Int, alpha: Float, onWallpaper: Int): Int =
        if (isSeeThrough(alpha)) onWallpaper else Contrast.readableOn(surfaceBase, DARK_TEXT, onWallpaper)

    @Synchronized
    fun resolve(context: Context, prefs: LauncherPrefs): ResolvedStyles {
        val variant = OneUiTheme.activeVariant(context, prefs)
        val night = variant.isDark
        val key = Key(
            prefs.styleConfig, prefs.paletteSource.name, prefs.paletteSeed, night,
            prefs.accentColor.name, prefs.drawerBgColor, prefs.folderBgColor,
            prefs.drawerBgOpacity, prefs.dockBgOpacity, wallpaperGeneration,
            variant.name
        )
        cachedStyles?.let { if (key == cachedKey) return it }

        val palette = paletteFor(context, prefs, key)
        val config = StyleConfig.decode(prefs.styleConfig)
        val accent = ThemeEngine.accentColor(context, prefs.accentColor)
        val themeText = if (night) 0xFFFFFFFF.toInt() else 0xFF11161D.toInt()
        val onWallpaper = context.getColor(R.color.onWallpaper)
        val themeSurface = computeThemeSurface(night, variant)

        val drawerLegacy = LabelStyler.parseColor(prefs.drawerBgColor, computeDrawerDefault(night, variant))
        val folderLegacy = LabelStyler.parseColor(
            prefs.folderBgColor,
            computeFolderDefault(night, variant, themeSurface)
        )

        val ctx = SurfaceContext(variant, palette, accent, themeText, onWallpaper, prefs)
        val styles = ResolvedStyles(
            buildSurfaceMap(ctx, drawerLegacy, folderLegacy, themeSurface)
        )
        cachedKey = key
        cachedStyles = styles
        return styles
    }

    private fun paletteFor(context: Context, prefs: LauncherPrefs, key: Key): TonalPalette {
        val paletteKey = key.copy(config = "", drawerBgColor = "", folderBgColor = "", drawerAlpha = 0f, dockAlpha = 0f)
        cachedPalette?.let { if (paletteKey == cachedPaletteKey) return it }
        val fallback = ThemeEngine.accentColor(context, prefs.accentColor)
        val palette = PaletteFactory.resolve(
            context, prefs.paletteSource, prefs.paletteSeed, fallback
        )
        cachedPaletteKey = paletteKey
        cachedPalette = palette
        return palette
    }
}

private const val MAX_ALPHA = 255f
private const val COLOR_MASK = 0x00FFFFFF
private const val SHIFT_ALPHA = 24
private const val MAX_COLOR_BYTE = 255
private const val OPAQUE_BYTE = 0xFF
private const val CORNER_COUNT = 4

private class SurfaceContext(
    val variant: ThemeStyleVariant,
    val palette: TonalPalette,
    val accent: Int,
    val themeText: Int,
    val onWallpaper: Int,
    val prefs: LauncherPrefs
) {
    val config: StyleConfig = StyleConfig.decode(prefs.styleConfig)
}

private fun buildSurfaceMap(
    ctx: SurfaceContext,
    drawerLegacy: Int,
    folderLegacy: Int,
    themeSurface: Int
): Map<SurfaceId, ResolvedSurface> {
    val drawer = resolveSurface(
        SurfaceId.DRAWER, drawerLegacy, ctx.prefs.drawerBgOpacity, ctx,
        textFor = { base, alpha -> Styles.drawerText(base, alpha, ctx.onWallpaper) }
    )
    val popup = resolveSurface(SurfaceId.POPUP, themeSurface, null, ctx)
    val cards = resolveSurface(
        SurfaceId.CARDS, popup.surface, null, ctx,
        cardText(ctx.config, ctx.onWallpaper, ctx.variant)
    )
    return mapOf(
        SurfaceId.DRAWER to drawer,
        SurfaceId.DRAWER_SEARCH_BAR to resolveSurface(
            SurfaceId.DRAWER_SEARCH_BAR,
            (Styles.DRAWER_FIELD_ALPHA shl SHIFT_ALPHA) or (drawer.text and COLOR_MASK),
            null, ctx
        ),
        SurfaceId.DOCK to resolveSurface(
            SurfaceId.DOCK, (OPAQUE_BYTE shl SHIFT_ALPHA) or Styles.DOCK_BASE_RGB, ctx.prefs.dockBgOpacity, ctx
        ),
        SurfaceId.FOLDER to resolveSurface(
            SurfaceId.FOLDER, folderLegacy, null, ctx
        ),
        SurfaceId.POPUP to popup,
        SurfaceId.CARDS to cards,
        SurfaceId.SEARCH_BAR to resolveSurface(
            SurfaceId.SEARCH_BAR, themeSurface, null, ctx
        ),
        SurfaceId.SETTINGS to resolveSurface(
            SurfaceId.SETTINGS, computeSettingsDefault(ctx.variant.isDark, ctx.variant), null, ctx
        ),
        SurfaceId.HOME to resolveSurface(
            SurfaceId.HOME, android.graphics.Color.TRANSPARENT, null, ctx
        )
    )
}

private fun computeThemeSurface(night: Boolean, variant: ThemeStyleVariant): Int = when {
    night && variant == ThemeStyleVariant.DARK_AMOLED -> OneUiTheme.AMOLED_BLACK
    night -> 0xFF1A2230.toInt()
    else -> 0xFFFFFFFF.toInt()
}

private fun computeDrawerDefault(night: Boolean, variant: ThemeStyleVariant): Int = when {
    night && variant == ThemeStyleVariant.DARK_AMOLED -> OneUiTheme.AMOLED_BLACK
    night -> 0xFF1E2735.toInt()
    else -> 0xFFFFFFFF.toInt()
}

private fun computeFolderDefault(night: Boolean, variant: ThemeStyleVariant, themeSurface: Int): Int = when {
    night -> themeSurface
    variant == ThemeStyleVariant.LIGHT_FROSTED -> OneUiTheme.FROSTED_SURFACE
    else -> 0xFFFFFFFF.toInt()
}

private fun computeSettingsDefault(night: Boolean, variant: ThemeStyleVariant): Int = when {
    night && variant == ThemeStyleVariant.DARK_AMOLED -> OneUiTheme.AMOLED_BLACK
    night -> 0xFF0D1117.toInt()
    else -> 0xFFF4F5F7.toInt()
}

private fun autoText(
    styled: Int?,
    surfaceBase: Int,
    alpha: Float?,
    auto: ((surfaceBase: Int, alpha: Float) -> Int)?,
    minContrast: Int?
): Int? {
    if (styled != null || auto == null) return null
    val effective = alpha ?: (android.graphics.Color.alpha(surfaceBase) / MAX_ALPHA)
    val text = auto(surfaceBase, effective)
    return if (Styles.isSeeThrough(effective)) text else Contrast.ensure(text, surfaceBase, minContrast)
}

private fun resolveSurface(
    id: SurfaceId,
    defaultSurface: Int,
    defaultAlpha: Float?,
    ctx: SurfaceContext,
    textFor: ((surfaceBase: Int, alpha: Float) -> Int)? = null
): ResolvedSurface {
    val style = ctx.config.styleFor(id, ctx.variant)
    val fallbackWithVariant = OneUiTheme.defaultSurfaceColor(ctx.variant, id, defaultSurface)
    val surfaceBase = colorFor(style.surface, ctx.palette) ?: fallbackWithVariant
    val alpha = style.alpha ?: defaultAlpha
    val surface = if (alpha != null) {
        ((alpha.coerceIn(0f, 1f) * MAX_COLOR_BYTE).toInt() shl SHIFT_ALPHA) or (surfaceBase and COLOR_MASK)
    } else {
        surfaceBase
    }
    val styledText = colorFor(style.text, ctx.palette)
    return ResolvedSurface(
        surface = surface,
        text = autoText(styledText, surfaceBase, alpha, textFor, style.minContrast)
            ?: Contrast.ensure(styledText ?: ctx.themeText, surfaceBase, style.minContrast),
        accent = colorFor(style.accent, ctx.palette) ?: ctx.accent,
        cornerRadiiDp = style.cornerRadii?.takeIf { id == SurfaceId.FOLDER }?.let { radii ->
            FloatArray(CORNER_COUNT) { radii.getOrElse(it) { 0f } }
        }
    )
}

private fun cardText(
    config: StyleConfig,
    onWallpaper: Int,
    variant: ThemeStyleVariant? = null
): ((Int, Float) -> Int)? =
    if (config.styleFor(SurfaceId.CARDS, variant).let { it.surface.isDefault && it.alpha == null }) {
        null
    } else {
        { base, alpha -> Styles.drawerText(base, alpha, onWallpaper) }
    }

private fun colorFor(spec: ColorSpec, palette: TonalPalette): Int? = when {
    spec.fixed != null -> runCatching { android.graphics.Color.parseColor(spec.fixed) }.getOrNull()
    spec.role != null && spec.tone != null -> palette.tone(spec.role, spec.tone)
    else -> null
}
