package com.ntdpz.pie.core.designsystem.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.provider.Settings
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

/** Small native-window sprite renderer. Its owner controls visibility and interaction. */
class CompanionSpriteView(
    context: Context,
    private val atlas: Bitmap,
    private val reactions: Bitmap,
    private val edges: Bitmap,
) : View(context) {
    var edge: CompanionEdge = CompanionEdge.Center
        set(value) { if (field != value) { field = value; edgeStartedAt = SystemClock.uptimeMillis(); invalidate() } }
    private var edgeStartedAt = 0L
    private var motionEnabled = true
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
            motionEnabled = !reducedMotion && scale > 0f
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
        if (edge != CompanionEdge.Center) {
            val progress = if (motionEnabled && !reducedMotion) {
                ((SystemClock.uptimeMillis() - edgeStartedAt) / 180f).coerceIn(0f, 1f)
            } else 1f
            val breath = if (motionEnabled && !reducedMotion && animation == CompanionAnimation.Idle) {
                1f - ((sin(frame * Math.PI / 4) + 1) * .0075).toFloat()
            } else 1f
            canvas.save()
            canvas.clipRect(0, 0, width, height)
            val horizontal = when (edge) {
                CompanionEdge.Left, CompanionEdge.TopLeft, CompanionEdge.BottomLeft -> -1
                CompanionEdge.Right, CompanionEdge.TopRight, CompanionEdge.BottomRight -> 1
                else -> 0
            }
            val vertical = if (edge.anchorY == 0f) -1 else if (edge.anchorY == 1f) 1 else 0
            canvas.translate(horizontal * width * .06f * (1f - progress), vertical * height * .06f * (1f - progress))
            canvas.scale(1f, breath, width / 2f, height * edge.anchorY)
            paint.alpha = (progress * 255).toInt()
            canvas.drawBitmap(
                edges, Rect(edge.column * 256, 0, (edge.column + 1) * 256, 256),
                Rect(0, 0, width, height), paint,
            )
            canvas.restore()
            paint.alpha = 255
            if (progress < 1f) postInvalidateOnAnimation()
            return
        }
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
