package com.ntdpz.pie.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.provider.Settings
import android.view.View

/** Small native-window sprite renderer. Its owner controls visibility and interaction. */
class CompanionSpriteView(context: Context, private val atlas: Bitmap) : View(context) {
    var animation: CompanionAnimation = CompanionAnimation.Idle
        set(value) {
            if (field != value) {
                field = value
                frame = 0
                restart()
            }
        }
    var reducedMotion: Boolean = false
        set(value) { field = value; restart() }

    private var frame = 0
    private var nextFrame = 0
    /** Restart visibly even when another touch arrives during the same reaction. */
    fun playTouchReaction() {
        animation = CompanionAnimation.Tap
        frame = 3
        restart()
    }

    private val paint = Paint().apply { isFilterBitmap = false }
    private val advance = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow || visibility != VISIBLE) return
            val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            if (reducedMotion || scale <= 0f) {
                frame = if (animation == CompanionAnimation.Tap) 5 else 0
                invalidate()
                postDelayed(this, 1_000)
                return
            }
            frame = nextFrame
            invalidate()
            val duration = when (animation) {
                CompanionAnimation.Idle -> intArrayOf(1_000, 1_000, 800, 120, 1_000, 800, 200, 1_000)[frame]
                CompanionAnimation.Rest -> 1_500
                else -> 180
            }
            if (animation == CompanionAnimation.Idle || animation == CompanionAnimation.Rest || frame < 7) {
                nextFrame = (frame + 1) % 8
                postDelayed(this, (duration * scale).toLong().coerceAtLeast(1))
            }
        }
    }

    private fun restart() {
        nextFrame = frame
        removeCallbacks(advance)
        if (isAttachedToWindow && visibility == VISIBLE) post(advance)
        invalidate()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); restart() }

    override fun onDetachedFromWindow() { removeCallbacks(advance); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(
            atlas, Rect(frame * 192, animation.ordinal * 208, (frame + 1) * 192, (animation.ordinal + 1) * 208),
            Rect(0, 0, width, height), paint,
        )
    }
}
