package com.supernova.launcher.ui

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.SystemClock
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.widget.ImageViewCompat
import com.supernova.launcher.R
import com.supernova.launcher.SettingsActivity
import com.supernova.launcher.runLogged

// The buttons at a search bar's right end. The home bar and the drawer field
// offer the same ones and draw them the same way; each keeps its own list
// (the home bar's config chips, the drawer's drawerActionButtons pref), in
// the order the buttons were turned on.
object SearchBarButtons {

    // How both bars' settings pages list them.
    val ROWS = listOf(
        SearchBarChip.DATE, SearchBarChip.WEATHER, SearchBarChip.CHROME,
        SearchBarChip.PLAY_STORE, SearchBarChip.ASSISTANT,
        SearchBarChip.VOICE, SearchBarChip.SETTINGS
    )

    // Sized like the magnifier on the left: a 36dp box with 8dp of padding
    // leaves its 20dp.
    const val BUTTON_DP = 36
    private const val BUTTON_PAD_DP = 8

    // Every search bar's icons (the magnifier and the buttons) sit a little
    // back from the bar's text colour: 80% of it.
    private const val ICON_ALPHA = 0xCC

    fun iconTint(color: Int): Int = androidx.core.graphics.ColorUtils.setAlphaComponent(color, ICON_ALPHA)

    @StringRes
    fun label(chip: SearchBarChip): Int = when (chip) {
        SearchBarChip.VOICE -> R.string.search_bar_chip_voice
        SearchBarChip.ASSISTANT -> R.string.search_bar_chip_assistant
        SearchBarChip.DATE -> R.string.search_bar_chip_date
        SearchBarChip.PLAY_STORE -> R.string.chip_play_store
        SearchBarChip.SETTINGS -> R.string.settings
        SearchBarChip.CHROME -> R.string.search_bar_chip_chrome
        SearchBarChip.WEATHER -> R.string.search_bar_chip_weather
    }

    // The drawer's stored list ("PLAY_STORE,SETTINGS"); unknown names drop.
    fun decode(raw: String): List<SearchBarChip> =
        raw.split(',').mapNotNull { SearchBarChip.fromName(it.trim()) }.distinct()

    fun encode(chips: List<SearchBarChip>): String = chips.joinToString(",") { it.name }

    // A list with one button turned on (added at the end) or off.
    fun toggled(chips: List<SearchBarChip>, chip: SearchBarChip, on: Boolean): List<SearchBarChip> {
        val without = chips.filterNot { it == chip }
        return if (on) without + chip else without
    }

    // A button for a bar: an icon in [color], or the date as text in it.
    // With no onClick it is a picture only (the settings previews).
    fun view(
        context: Context,
        chip: SearchBarChip,
        color: Int,
        coloredBrandIcons: Boolean = false,
        onClick: (() -> Unit)?
    ): View {
        val pad = context.dp(BUTTON_PAD_DP)
        val isBrandChip = chip == SearchBarChip.PLAY_STORE ||
            chip == SearchBarChip.CHROME || chip == SearchBarChip.WEATHER
        val useColor = coloredBrandIcons && isBrandChip
        val iconRes = resolveIcon(context, chip, useColor)
        val view = if (iconRes == null) {
            TextView(context).apply {
                textSize = DATE_SP
                maxLines = 1
                gravity = Gravity.CENTER
                setPadding(context.dp(DATE_PAD_DP), pad, context.dp(DATE_PAD_DP), pad)
                setTextColor(color)
                text = dateText(context)
            }
        } else {
            ImageView(context).apply {
                setImageResource(iconRes)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(pad, pad, pad, pad)
                if (useColor) {
                    ImageViewCompat.setImageTintList(this, null)
                } else {
                    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(iconTint(color)))
                }
            }
        }
        if (onClick == null) {
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        } else {
            view.contentDescription = context.getString(label(chip))
            view.background = borderlessRipple(context)
            view.setOnClickListener { onClick() }
        }
        return view
    }

    fun dateText(context: Context): String = DateUtils.formatDateTime(
        context, System.currentTimeMillis(),
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR
    )

    fun openChromeSearch(context: Context) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            setPackage("com.android.chrome")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (runCatching { context.startActivity(intent) }.isFailure) {
            val fallback = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri()).apply {
                setPackage("com.android.chrome")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (runCatching { context.startActivity(fallback) }.isFailure) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_WEB_SEARCH).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }
            }
        }
    }

    // What a tap does. Each is blind-started: a missing handler just no-ops.
    fun run(context: Context, chip: SearchBarChip) {
        when (chip) {
            SearchBarChip.VOICE -> VoiceSearch.start(context)
            // Same ladder as the ASSISTANT gesture action, with the
            // voice-command front door first.
            SearchBarChip.ASSISTANT -> {
                if (runCatching { context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND)) }.isFailure) {
                    runLogged("open assistant") { context.startActivity(Intent(Intent.ACTION_ASSIST)) }
                }
            }
            // The documented "open calendar at a day" deep link.
            SearchBarChip.DATE -> {
                val uri = CalendarContract.CONTENT_URI.buildUpon()
                    .appendPath("time")
                    .also { ContentUris.appendId(it, System.currentTimeMillis()) }
                    .build()
                runLogged("open link") { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }
            SearchBarChip.PLAY_STORE -> runLogged("open Play Store") {
                context.packageManager.getLaunchIntentForPackage("com.android.vending")
                    ?.let(context::startActivity)
            }
            SearchBarChip.SETTINGS -> context.startActivity(Intent(context, SettingsActivity::class.java))
            SearchBarChip.CHROME -> openChromeSearch(context)
            SearchBarChip.WEATHER -> openWeatherApp(context)
        }
    }

    private const val DATE_SP = 14f
    private const val DATE_PAD_DP = 6
}

fun openWeatherApp(context: Context) {
    val samsungWeather = context.packageManager.getLaunchIntentForPackage("com.sec.android.daemonapp")
    if (samsungWeather != null && runCatching { context.startActivity(samsungWeather) }.isSuccess) return

    val googleWeatherIntent = Intent(Intent.ACTION_VIEW).apply {
        data = "dynact://velour/weather/ProxyActivity".toUri()
        setPackage("com.google.android.googlequicksearchbox")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (runCatching { context.startActivity(googleWeatherIntent) }.isSuccess) return

    val googleWeatherSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
        putExtra(android.app.SearchManager.QUERY, "weather")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(googleWeatherSearch) }
}

fun openSamsungFinder(context: Context) {
    val intents = listOfNotNull(
        Intent().apply {
            setClassName("com.sec.android.app.launcher", "com.sec.android.app.launcher.search.SearchActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent("com.sec.android.app.launcher.action.OPEN_FINDER").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent("com.samsung.android.app.galaxyfinder.LAUNCH_SFINDER").apply {
            setPackage("com.samsung.android.app.galaxyfinder")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent("com.samsung.android.app.galaxyfinder.LAUNCH_SFINDER").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent().apply {
            setClassName(
                "com.samsung.android.app.galaxyfinder",
                "com.samsung.android.app.galaxyfinder.GalaxyFinderActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent().apply {
            setClassName(
                "com.samsung.android.app.galaxyfinder",
                "com.samsung.android.app.galaxyfinder.MainActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.galaxyfinder")?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.sec.android.app.launcher")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.samsung.android.app.galaxyfinder")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    for (intent in intents) {
        if (runCatching { context.startActivity(intent) }.isSuccess) {
            return
        }
    }
}

private const val DAY_START_HOUR = 6
private const val DAY_END_HOUR = 19
private const val WEATHER_CACHE_TTL_MS = 300_000L

enum class WeatherCondition {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAIN,
    SNOW,
    THUNDER,
    WIND
}

private var cachedCondition: WeatherCondition? = null
private var lastConditionQueryMs: Long = 0L

private val SAMSUNG_WEATHER_URIS = listOf(
    "content://com.sec.android.daemonapp.ap.weatherinfo/weatherinfo",
    "content://com.sec.android.daemonapp.provider/weatherinfo",
    "content://com.samsung.android.weather.provider/weatherinfo",
    "content://com.sec.android.daemonapp.ap.accuweather.provider/weatherinfo"
)

@Suppress("MagicNumber")
private fun mapIconCode(code: Int): WeatherCondition? = when (code) {
    1, 2, 33, 34 -> WeatherCondition.SUNNY
    3, 4, 5, 35, 36, 37 -> WeatherCondition.PARTLY_CLOUDY
    6, 7, 8, 38 -> WeatherCondition.CLOUDY
    12, 13, 14, 18, 26, 29, 39, 40 -> WeatherCondition.RAIN
    19, 20, 21, 22, 23, 24, 25, 43, 44 -> WeatherCondition.SNOW
    15, 16, 17, 41, 42 -> WeatherCondition.THUNDER
    32 -> WeatherCondition.WIND
    else -> null
}

@Suppress("CyclomaticComplexMethod")
private fun mapWeatherText(text: String): WeatherCondition? = when {
    "thunder" in text || "storm" in text || "lightning" in text -> WeatherCondition.THUNDER
    "snow" in text || "flurr" in text || "blizzard" in text || "sleet" in text || "ice" in text -> WeatherCondition.SNOW
    "rain" in text || "shower" in text || "drizzle" in text -> WeatherCondition.RAIN
    "wind" in text || "gale" in text || "breeze" in text -> WeatherCondition.WIND
    "cloud" in text || "overcast" in text || "fog" in text || "haze" in text || "mist" in text -> {
        if ("partly" in text || "mostly sunny" in text || "scattered" in text) {
            WeatherCondition.PARTLY_CLOUDY
        } else {
            WeatherCondition.CLOUDY
        }
    }
    "sun" in text || "clear" in text -> {
        if ("partly" in text || "mostly cloudy" in text) WeatherCondition.PARTLY_CLOUDY else WeatherCondition.SUNNY
    }
    else -> null
}

@Suppress("NestedBlockDepth")
private fun parseWeatherCursor(cursor: android.database.Cursor): WeatherCondition? {
    val iconColumns = listOf("COL_WEATHER_ICON_NUM", "WEATHER_ICON_NUM", "ICON_NUM", "AW_WEATHER_CODE", "icon")
    var result: WeatherCondition? = null
    for (col in iconColumns) {
        val idx = cursor.getColumnIndex(col)
        if (idx >= 0 && !cursor.isNull(idx)) {
            result = mapIconCode(cursor.getInt(idx))
            if (result != null) break
        }
    }
    if (result == null) {
        val textColumns = listOf("COL_WEATHER_TEXT", "WEATHER_TEXT", "WEATHER_CONDITION", "WEATHER_KEY", "description")
        for (col in textColumns) {
            val idx = cursor.getColumnIndex(col)
            if (idx >= 0 && !cursor.isNull(idx)) {
                result = mapWeatherText(cursor.getString(idx)?.lowercase() ?: "")
                if (result != null) break
            }
        }
    }
    return result
}

fun queryWeatherCondition(context: Context): WeatherCondition {
    val now = SystemClock.elapsedRealtime()
    val cached = cachedCondition
    if (cached != null && now - lastConditionQueryMs < WEATHER_CACHE_TTL_MS) {
        return cached
    }
    lastConditionQueryMs = now

    var found: WeatherCondition? = null
    for (uriString in SAMSUNG_WEATHER_URIS) {
        found = runCatching {
            val uri = uriString.toUri()
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) parseWeatherCursor(cursor) else null
            }
        }.getOrNull()
        if (found != null) break
    }
    if (found == null) {
        found = runCatching {
            val oemUri = "content://lineageos.weather/weather".toUri()
            context.contentResolver.query(oemUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) parseWeatherCursor(cursor) else null
            }
        }.getOrNull()
    }
    val result = found ?: WeatherCondition.PARTLY_CLOUDY
    cachedCondition = result
    return result
}

@Suppress("CyclomaticComplexMethod")
@DrawableRes
private fun resolveWeatherIcon(context: Context, colored: Boolean): Int {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val isNight = hour < DAY_START_HOUR || hour >= DAY_END_HOUR
    val condition = queryWeatherCondition(context)
    return when (condition) {
        WeatherCondition.SUNNY -> if (isNight) {
            if (colored) R.drawable.ic_weather_clear_night_color else R.drawable.ic_weather_clear_night
        } else {
            if (colored) R.drawable.ic_weather_sunny_color else R.drawable.ic_weather_sunny
        }
        WeatherCondition.PARTLY_CLOUDY -> if (isNight) {
            if (colored) R.drawable.ic_weather_color_night else R.drawable.ic_weather_night
        } else {
            if (colored) R.drawable.ic_weather_color else R.drawable.ic_weather
        }
        WeatherCondition.CLOUDY -> if (colored) R.drawable.ic_weather_cloudy_color else R.drawable.ic_weather_cloudy
        WeatherCondition.RAIN -> if (colored) R.drawable.ic_weather_rain_color else R.drawable.ic_weather_rain
        WeatherCondition.SNOW -> if (colored) R.drawable.ic_weather_snow_color else R.drawable.ic_weather_snow
        WeatherCondition.THUNDER -> if (colored) R.drawable.ic_weather_thunder_color else R.drawable.ic_weather_thunder
        WeatherCondition.WIND -> if (colored) R.drawable.ic_weather_wind_color else R.drawable.ic_weather_wind
    }
}

@DrawableRes
private fun resolveIcon(context: Context, chip: SearchBarChip, colored: Boolean): Int? = when (chip) {
    SearchBarChip.VOICE -> R.drawable.ic_mic
    SearchBarChip.ASSISTANT -> R.drawable.ic_assistant
    SearchBarChip.DATE -> null
    SearchBarChip.PLAY_STORE -> if (colored) R.drawable.ic_play_store_color else R.drawable.ic_play_store
    SearchBarChip.SETTINGS -> R.drawable.ic_tab_settings
    SearchBarChip.CHROME -> if (colored) R.drawable.ic_chrome_color else R.drawable.ic_chrome
    SearchBarChip.WEATHER -> resolveWeatherIcon(context, colored)
}

private fun borderlessRipple(context: Context) =
    TypedValue().takeIf {
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, it, true)
    }?.resourceId?.takeIf { it != 0 }?.let { AppCompatResources.getDrawable(context, it) }
