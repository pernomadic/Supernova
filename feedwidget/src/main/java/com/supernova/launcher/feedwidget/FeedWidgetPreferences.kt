package com.supernova.launcher.feedwidget

import android.content.Context
import android.content.SharedPreferences

enum class WidgetTheme(val id: String) {
    DARK_DEFAULT("dark_default"),
    DARK_AMOLED("dark_amoled"),
    LIGHT_DEFAULT("light_default"),
    LIGHT_FROSTED("light_frosted");

    companion object {
        fun fromId(id: String?): WidgetTheme = entries.find { it.id == id } ?: DARK_DEFAULT
    }
}

object FeedWidgetPreferences {
    private const val PREFS_NAME = "com.supernova.launcher.feedwidget.prefs"
    private const val KEY_THEME_PREFIX = "widget_theme_"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTheme(context: Context, appWidgetId: Int): WidgetTheme {
        val id = prefs(context).getString(KEY_THEME_PREFIX + appWidgetId, null)
        return WidgetTheme.fromId(id)
    }

    fun setTheme(context: Context, appWidgetId: Int, theme: WidgetTheme) {
        prefs(context).edit().putString(KEY_THEME_PREFIX + appWidgetId, theme.id).apply()
    }

    fun deleteWidgetPref(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(KEY_THEME_PREFIX + appWidgetId).apply()
    }
}
