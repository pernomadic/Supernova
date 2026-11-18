package com.supernova.launcher.feedwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.supernova.launcher.feedwidget.databinding.ActivityFeedWidgetConfigBinding

class FeedWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var binding: ActivityFeedWidgetConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        binding = ActivityFeedWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val currentTheme = FeedWidgetPreferences.getTheme(this, appWidgetId)
        when (currentTheme) {
            WidgetTheme.DARK_DEFAULT -> binding.themeDark.isChecked = true
            WidgetTheme.DARK_AMOLED -> binding.themeAmoled.isChecked = true
            WidgetTheme.LIGHT_DEFAULT -> binding.themeLight.isChecked = true
            WidgetTheme.LIGHT_FROSTED -> binding.themeFrosted.isChecked = true
        }

        binding.saveButton.setOnClickListener {
            val selectedTheme = when (binding.themeGroup.checkedRadioButtonId) {
                R.id.themeAmoled -> WidgetTheme.DARK_AMOLED
                R.id.themeLight -> WidgetTheme.LIGHT_DEFAULT
                R.id.themeFrosted -> WidgetTheme.LIGHT_FROSTED
                else -> WidgetTheme.DARK_DEFAULT
            }
            FeedWidgetPreferences.setTheme(this, appWidgetId, selectedTheme)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            FeedAppWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
