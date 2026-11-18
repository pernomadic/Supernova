package com.supernova.launcher.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

// Aryonova's animations follow its own "Disable animations" switch and
// developer options scales (0.25x to 10x) for window animation scale,
// transition animation scale, and animator duration scale.
object AnimationScale {

    private const val TAG = "AnimationScale"
    private const val MIN_SYSTEM_SCALE = 0.25f
    private const val MAX_SYSTEM_SCALE = 10f

    private val main = Handler(Looper.getMainLooper())

    // The launcher's own switch, from the settings.
    @Volatile private var launcherAnimations = true

    // The scale the system last gave this process.
    @Volatile private var systemScale = 1f

    @Volatile private var animatorScale = 1f
    @Volatile private var transitionScale = 1f
    @Volatile private var windowScale = 1f

    // True while this object is the one setting the scale.
    @Volatile private var settingOwn = false

    private var installed = false

    private val setter by lazy { findSetter() }

    // Hidden API on purpose (see above); a refusal leaves setter null.
    @SuppressLint("DiscouragedPrivateApi")
    private fun findSetter() = runCatching {
        ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.javaPrimitiveType)
            .apply { isAccessible = true }
    }.onFailure { Log.w(TAG, "No setDurationScale", it) }.getOrNull()

    // Returns the effective scale multiplier taking developer options scale (0.25x-10x) into account.
    fun effectiveScale(): Float = if (!launcherAnimations) 0f else systemScale.coerceIn(0f, MAX_SYSTEM_SCALE)

    // Whether the system's animations are off: what the first run asks about.
    fun systemAnimationsOff(context: Context): Boolean =
        systemScale == 0f || settingScale(context) == 0f

    fun install(app: Application) {
        if (installed) return
        installed = true
        systemScale = settingScale(app)
        // The first window of a process takes the system's scale; each
        // resume puts this one back after it.
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                activity.window.decorView.post(::apply)
            }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })

        val observer = object : ContentObserver(main) {
            override fun onChange(selfChange: Boolean) {
                systemScale = settingScale(app)
                apply()
            }
        }
        val resolver = app.contentResolver
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE), false, observer
        )
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.WINDOW_ANIMATION_SCALE), false, observer
        )
    }

    // The launcher's switch changed (or was read).
    fun setLauncherAnimations(on: Boolean) {
        launcherAnimations = on
        if (Looper.myLooper() == Looper.getMainLooper()) apply() else main.post(::apply)
    }

    // Puts this process's scale where it belongs now. Respects system Developer Option
    // scales (0.25x to 10x) for window, transition, and animator duration.
    fun apply() {
        val wanted = if (!launcherAnimations) 0f else systemScale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ValueAnimator.getDurationScale() == wanted) return
        val method = setter ?: return
        settingOwn = true
        try {
            method.invoke(null, wanted)
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "setDurationScale refused", e)
        } finally {
            settingOwn = false
        }
    }

    private fun settingScale(context: Context): Float {
        val resolver = context.contentResolver
        val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        val window = Settings.Global.getFloat(resolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
        animatorScale = animator
        transitionScale = transition
        windowScale = window
        val chosen = when {
            animator != 1f -> animator
            transition != 1f -> transition
            window != 1f -> window
            else -> 1f
        }
        return if (chosen <= 0f) 0f else chosen.coerceIn(MIN_SYSTEM_SCALE, MAX_SYSTEM_SCALE)
    }
}
