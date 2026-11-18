package com.supernova.launcher.feedwidget

import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.supernova.launcher.feedwidget.databinding.ActivityFeedFullscreenBinding

class FeedFullscreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedFullscreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val themeId = intent.getStringExtra(EXTRA_THEME_ID)
        val theme = WidgetTheme.fromId(themeId)
        applyThemeStyle(theme)
        applyWindowBlur()

        binding.closeBtn.setOnClickListener {
            finish()
        }

        binding.searchBar.setOnClickListener {
            runCatching {
                val searchIntent = Intent(Intent.ACTION_WEB_SEARCH)
                startActivity(searchIntent)
            }
        }
    }

    private fun applyThemeStyle(theme: WidgetTheme) {
        val isDark = theme == WidgetTheme.DARK_DEFAULT || theme == WidgetTheme.DARK_AMOLED
        val bgTint = when (theme) {
            WidgetTheme.DARK_AMOLED -> ContextCompat.getColor(this, R.color.oneui_amoled_bg)
            WidgetTheme.DARK_DEFAULT -> ContextCompat.getColor(this, R.color.oneui_dark_bg)
            WidgetTheme.LIGHT_DEFAULT -> ContextCompat.getColor(this, R.color.oneui_light_bg)
            WidgetTheme.LIGHT_FROSTED -> ContextCompat.getColor(this, R.color.oneui_frosted_light_bg)
        }
        binding.fullscreenRoot.setBackgroundColor(bgTint)

        val cardBg = if (isDark) R.drawable.bg_card_plate_dark else R.drawable.bg_card_plate_light
        binding.card1.setBackgroundResource(cardBg)
        binding.card2.setBackgroundResource(cardBg)
        binding.card3.setBackgroundResource(cardBg)

        val searchBg = if (isDark) R.drawable.bg_search_pill_dark else R.drawable.bg_search_pill_light
        binding.searchBar.setBackgroundResource(searchBg)

        val primaryColor = ContextCompat.getColor(
            this,
            if (isDark) R.color.text_dark_primary else R.color.text_light_primary
        )
        val secondaryColor = ContextCompat.getColor(
            this,
            if (isDark) R.color.text_dark_secondary else R.color.text_light_secondary
        )

        binding.headerTitle.setTextColor(primaryColor)
        binding.searchPrompt.setTextColor(secondaryColor)
        binding.fsCard1Title.setTextColor(primaryColor)
        binding.fsCard1Body.setTextColor(secondaryColor)
        binding.fsCard2Title.setTextColor(primaryColor)
        binding.fsCard2Body.setTextColor(secondaryColor)
        binding.fsCard3Title.setTextColor(primaryColor)
        binding.fsCard3Body.setTextColor(secondaryColor)
    }

    private fun applyWindowBlur() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                setBlurBehindRadius(BLUR_RADIUS_PX)
            }
            binding.fullscreenRoot.setRenderEffect(
                RenderEffect.createBlurEffect(
                    BLUR_RADIUS_FLOAT, BLUR_RADIUS_FLOAT, Shader.TileMode.CLAMP
                )
            )
        }
    }

    companion object {
        const val EXTRA_THEME_ID = "extra_theme_id"
        private const val BLUR_RADIUS_PX = 250
        private const val BLUR_RADIUS_FLOAT = 250f
    }
}
