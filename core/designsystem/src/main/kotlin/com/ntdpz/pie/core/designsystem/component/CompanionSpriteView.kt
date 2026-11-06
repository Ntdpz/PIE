package com.ntdpz.pie.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.provider.Settings
import android.view.View

/** Small native-window sprite renderer. Its owner controls visibility and interaction. */
class CompanionSpriteView(context: Context, private val atlas: Bitmap, private val reactions: Bitmap) : View(context) {
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
    fun playReaction(action: CompanionAnimation) {
        animation = action
        frame = 0
        restart()
    }

    private val paint = Paint().apply { isFilterBitmap = false }
    private val advance = object : Runnable {
        override fun run() {
            if (!isAttachedToWindow || visibility != VISIBLE) return
            val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                .takeIf { it.isFinite() && it >= 0f } ?: 1f
            if (reducedMotion || scale <= 0f) {
                frame = animation.stillFrame
                invalidate()
                postDelayed(this, 1_000)
                return
            }
            frame = nextFrame
            invalidate()
            val duration = animation.frameDuration(frame)
            if (animation.loops || frame < 7) {
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
        val scale = minOf(width, height) / 256f
        val drawWidth = (animation.sourceWidth * scale).toInt()
        val drawHeight = (animation.sourceHeight * scale).toInt()
        val left = (width - drawWidth) / 2
        canvas.drawBitmap(
            if (animation.reactionAtlas) reactions else atlas,
            Rect(frame * animation.sourceWidth, animation.row * animation.sourceHeight, (frame + 1) * animation.sourceWidth, (animation.row + 1) * animation.sourceHeight),
            Rect(left, height - drawHeight, left + drawWidth, height), paint,
        )
    }
}
