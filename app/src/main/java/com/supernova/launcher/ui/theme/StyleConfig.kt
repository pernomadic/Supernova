package com.supernova.launcher.ui.theme

import com.supernova.launcher.runLogged
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Surfaces the style engine can recolor. SEARCH_BAR and POPUP are declared
// ahead of their consumers (phases 4 and 6) so backups written then decode
// now and vice versa.
@Serializable
// HOME is the home screen's own background: a color laid over the wallpaper,
// transparent unless one is picked. CARDS is the plate of every card (the
// feed's, the drawer's and search's), which wore POPUP's colours before it
// had its own and still does until it is styled.
enum class SurfaceId { DRAWER, DOCK, FOLDER, POPUP, SEARCH_BAR, DRAWER_SEARCH_BAR, SETTINGS, HOME, CARDS }

// Tonal-palette roles, matching Material's accent1/neutral1/neutral2 ramps.
@Serializable
enum class PaletteRole { PRIMARY, NEUTRAL, NEUTRAL_VARIANT }

// One color slot: a tone from the active palette, a fixed ARGB, or default
// (fall through to the legacy pref, then the theme).
@Serializable
data class ColorSpec(
    val role: PaletteRole? = null,
    val tone: Int? = null,
    // "#AARRGGBB" or "#RRGGBB"
    val fixed: String? = null
) {
    val isDefault: Boolean get() = fixed == null && (role == null || tone == null)
}

@Serializable
data class SurfaceStyle(
    val surface: ColorSpec = ColorSpec(),
    val text: ColorSpec = ColorSpec(),
    val accent: ColorSpec = ColorSpec(),
    // null = keep the surface's legacy/default opacity.
    val alpha: Float? = null,
    // Corner radii in dp, [topLeft, topRight, bottomRight, bottomLeft];
    // null = the surface's built-in shape.
    val cornerRadii: List<Float>? = null,
    // Minimum contrast: the least tone gap (Contrast) the text keeps
    // from the surface, shading itself to hold it; null = off.
    val minContrast: Int? = null,
    // Blur effect percentage (0..100); null = default.
    val blur: Int? = null
) {
    fun isEmpty(): Boolean =
        surface.isDefault && text.isDefault && accent.isDefault &&
            alpha == null && cornerRadii == null && minContrast == null && blur == null
}

// The whole per-surface style document, persisted as one JSON pref
// (StoreKeys.styleConfig). An empty config must reproduce the pre-style-engine
// rendering exactly.
@Serializable
data class StyleConfig(
    val surfaces: Map<SurfaceId, SurfaceStyle> = emptyMap(),
    val variantSurfaces: Map<String, Map<SurfaceId, SurfaceStyle>> = emptyMap()
) {

    fun styleFor(id: SurfaceId, variant: ThemeStyleVariant? = null): SurfaceStyle {
        if (variant != null) {
            variantSurfaces[variant.storageKey]?.get(id)?.let { return it }
        }
        return surfaces[id] ?: EMPTY_SURFACE
    }

    fun with(id: SurfaceId, style: SurfaceStyle, variant: ThemeStyleVariant? = null): StyleConfig {
        if (variant == null) {
            val updated = if (style.isEmpty()) surfaces - id else surfaces + (id to style)
            return copy(surfaces = updated)
        }
        val currentVariantMap = variantSurfaces[variant.storageKey].orEmpty()
        val updatedVariantMap = if (style.isEmpty()) currentVariantMap - id else currentVariantMap + (id to style)
        val newVariants = if (updatedVariantMap.isEmpty()) {
            variantSurfaces - variant.storageKey
        } else {
            variantSurfaces + (variant.storageKey to updatedVariantMap)
        }
        return copy(variantSurfaces = newVariants)
    }

    fun encode(): String =
        if (surfaces.isEmpty() && variantSurfaces.isEmpty()) "" else json.encodeToString(serializer(), this)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        val EMPTY = StyleConfig()
        private val EMPTY_SURFACE = SurfaceStyle()

        fun decode(raw: String): StyleConfig =
            if (raw.isBlank()) EMPTY
            else runLogged("read style settings") { json.decodeFromString(serializer(), raw) }.getOrDefault(EMPTY)
    }
}
