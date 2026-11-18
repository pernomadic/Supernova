package com.supernova.launcher.feedwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            FeedWidgetPreferences.deleteWidgetPref(context, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val theme = FeedWidgetPreferences.getTheme(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_feed)

            val isDark = theme == WidgetTheme.DARK_DEFAULT || theme == WidgetTheme.DARK_AMOLED

            val bgDrawable = when (theme) {
                WidgetTheme.DARK_AMOLED -> R.drawable.bg_widget_feed_amoled
                WidgetTheme.DARK_DEFAULT -> R.drawable.bg_widget_feed_dark
                WidgetTheme.LIGHT_DEFAULT -> R.drawable.bg_widget_feed_light
                WidgetTheme.LIGHT_FROSTED -> R.drawable.bg_widget_feed_frosted
            }
            views.setInt(R.id.widgetRoot, "setBackgroundResource", bgDrawable)

            val cardPlateDrawable = if (isDark) R.drawable.bg_card_plate_dark else R.drawable.bg_card_plate_light
            views.setInt(R.id.widgetCard1, "setBackgroundResource", cardPlateDrawable)
            views.setInt(R.id.widgetCard2, "setBackgroundResource", cardPlateDrawable)

            val searchPillDrawable = if (isDark) R.drawable.bg_search_pill_dark else R.drawable.bg_search_pill_light
            views.setInt(R.id.widgetSearchPill, "setBackgroundResource", searchPillDrawable)

            val primaryColor = ContextCompat.getColor(
                context,
                if (isDark) R.color.text_dark_primary else R.color.text_light_primary
            )
            val secondaryColor = ContextCompat.getColor(
                context,
                if (isDark) R.color.text_dark_secondary else R.color.text_light_secondary
            )

            views.setTextColor(R.id.widgetTitle, primaryColor)
            views.setTextColor(R.id.widgetDate, secondaryColor)
            views.setTextColor(R.id.widgetSearchText, secondaryColor)
            views.setTextColor(R.id.card1Title, primaryColor)
            views.setTextColor(R.id.card1Content, secondaryColor)
            views.setTextColor(R.id.card2Title, primaryColor)
            views.setTextColor(R.id.card2Content, secondaryColor)

            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            views.setTextViewText(R.id.widgetDate, dateFormat.format(Date()))

            // Intent to open full screen Feed
            val fullscreenIntent = Intent(context, FeedFullscreenActivity::class.java).apply {
                putExtra(FeedFullscreenActivity.EXTRA_THEME_ID, theme.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val fullscreenPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                fullscreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetTitle, fullscreenPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetSearchPill, fullscreenPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetCard1, fullscreenPendingIntent)
            views.setOnClickPendingIntent(R.id.widgetCard2, fullscreenPendingIntent)

            // Intent to configure widget
            val configIntent = Intent(context, FeedWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + CONFIG_REQUEST_OFFSET,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetSettingsBtn, configPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private const val CONFIG_REQUEST_OFFSET = 10000
    }
}
