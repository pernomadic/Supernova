package com.supernova.launcher.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

// Plays Samsung OneUI-style app return transition: the closing app window
// smoothly minimizes and collapses from fullscreen directly into the original
// icon position, while the home screen settles into focus.
object OneUiMinimizeTransition {

    private const val DURATION_BASE_MS = 280L
    private const val SETTLE_DURATION_BASE_MS = 160L
    private const val INTERPOLATOR_X1 = 0.22f
    private const val INTERPOLATOR_Y1 = 0.25f
    private const val SCREEN_CORNER_RADIUS_DP = 32f
    private const val SQUIRCLE_RADIUS_RATIO = 0.28f
    private const val WORKSPACE_START_SCALE = 0.94f
    private const val WORKSPACE_START_ALPHA = 0.82f
    private const val ICON_MIN_SCALE = 0.88f
    private const val ICON_OVERSHOOT_TENSION = 1.5f
    private const val FADE_OUT_START_PROGRESS = 0.75f
    private const val CARD_ALPHA = 240
    private const val MAX_ALPHA = 255
    private const val ICON_SCALE_MULTIPLIER = 1.25f

    data class TargetAnchor(
        val view: View?,
        val boundsOnScreen: Rect,
        val bitmap: Bitmap?,
        val dominantColor: Int
    )

    fun play(
        root: ViewGroup,
        workspace: View,
        dock: View?,
        target: TargetAnchor,
        speedFactor: Float
    ) {
        val rootLoc = IntArray(2).also { root.getLocationOnScreen(it) }
        val targetRect = Rect(target.boundsOnScreen).apply { offset(-rootLoc[0], -rootLoc[1]) }
        val screenWidth = if (root.width > 0) root.width else root.resources.displayMetrics.widthPixels
        val screenHeight = if (root.height > 0) root.height else root.resources.displayMetrics.heightPixels
        val startRect = Rect(0, 0, screenWidth, screenHeight)

        val density = root.resources.displayMetrics.density
        val startRadius = SCREEN_CORNER_RADIUS_DP * density
        val targetRadius = targetRect.width() * SQUIRCLE_RADIUS_RATIO

        val duration = (DURATION_BASE_MS * speedFactor).toLong().coerceAtLeast(1L)
        val interpolator = PathInterpolator(INTERPOLATOR_X1, INTERPOLATOR_Y1, 0f, 1f)

        target.view?.alpha = 0f

        val cardDrawable = MinimizeCardDrawable(
            startRect, targetRect, target.bitmap, target.dominantColor, startRadius, targetRadius
        )
        root.overlay.add(cardDrawable)

        animateWorkspaceEntrance(workspace, dock, duration, interpolator)

        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            this.interpolator = interpolator
            addUpdateListener {
                cardDrawable.progress = it.animatedFraction
                cardDrawable.invalidateSelf()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishTransition(root, cardDrawable, target.view, speedFactor)
                }

                override fun onAnimationCancel(animation: Animator) {
                    finishTransition(root, cardDrawable, target.view, speedFactor)
                }
            })
        }
        animator.start()
    }

    private fun finishTransition(
        root: ViewGroup,
        cardDrawable: Drawable,
        anchor: View?,
        speedFactor: Float
    ) {
        root.overlay.remove(cardDrawable)
        if (anchor != null) {
            anchor.alpha = 1f
            anchor.scaleX = ICON_MIN_SCALE
            anchor.scaleY = ICON_MIN_SCALE
            val settleDuration = (SETTLE_DURATION_BASE_MS * speedFactor).toLong().coerceAtLeast(1L)
            anchor.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(settleDuration)
                .setInterpolator(OvershootInterpolator(ICON_OVERSHOOT_TENSION))
                .start()
        }
    }

    private fun animateWorkspaceEntrance(
        workspace: View,
        dock: View?,
        duration: Long,
        interpolator: PathInterpolator
    ) {
        workspace.scaleX = WORKSPACE_START_SCALE
        workspace.scaleY = WORKSPACE_START_SCALE
        workspace.alpha = WORKSPACE_START_ALPHA
        workspace.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(duration)
            .setInterpolator(interpolator)
            .start()

        dock?.let {
            it.scaleX = WORKSPACE_START_SCALE
            it.scaleY = WORKSPACE_START_SCALE
            it.alpha = WORKSPACE_START_ALPHA
            it.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(duration)
                .setInterpolator(interpolator)
                .start()
        }
    }

    private class MinimizeCardDrawable(
        private val startRect: Rect,
        private val targetRect: Rect,
        private val iconBitmap: Bitmap?,
        dominantColor: Int,
        private val startRadius: Float,
        private val targetRadius: Float
    ) : Drawable() {

        var progress: Float = 0f

        private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dominantColor != 0 && dominantColor != Color.TRANSPARENT) {
                dominantColor
            } else {
                Color.DKGRAY
            }
            alpha = CARD_ALPHA
        }

        private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val cardRect = RectF()
        private val iconDest = RectF()

        override fun draw(canvas: Canvas) {
            val p = progress.coerceIn(0f, 1f)
            val left = startRect.left + (targetRect.left - startRect.left) * p
            val top = startRect.top + (targetRect.top - startRect.top) * p
            val right = startRect.right + (targetRect.right - startRect.right) * p
            val bottom = startRect.bottom + (targetRect.bottom - startRect.bottom) * p
            cardRect.set(left, top, right, bottom)

            val currentRadius = startRadius + (targetRadius - startRadius) * p

            val cardAlpha = when {
                p > FADE_OUT_START_PROGRESS -> {
                    val fadeRatio = (1f - p) / (1f - FADE_OUT_START_PROGRESS)
                    (CARD_ALPHA * fadeRatio).toInt().coerceIn(0, MAX_ALPHA)
                }
                else -> CARD_ALPHA
            }

            cardPaint.alpha = cardAlpha
            canvas.drawRoundRect(cardRect, currentRadius, currentRadius, cardPaint)

            if (iconBitmap != null && !iconBitmap.isRecycled) {
                drawCenteredIcon(canvas, cardRect, p, cardAlpha)
            }
        }

        private fun drawCenteredIcon(canvas: Canvas, rect: RectF, p: Float, cardAlpha: Int) {
            val icon = iconBitmap ?: return
            val targetSize = targetRect.width().toFloat()
            val startSize = targetSize * ICON_SCALE_MULTIPLIER
            val currentSize = startSize + (targetSize - startSize) * p
            val cx = rect.centerX()
            val cy = rect.centerY()
            iconDest.set(
                cx - currentSize / 2f,
                cy - currentSize / 2f,
                cx + currentSize / 2f,
                cy + currentSize / 2f
            )
            iconPaint.alpha = cardAlpha
            canvas.drawBitmap(icon, null, iconDest, iconPaint)
        }

        override fun setAlpha(alpha: Int) {
            cardPaint.alpha = alpha
            iconPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            cardPaint.colorFilter = colorFilter
            iconPaint.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
