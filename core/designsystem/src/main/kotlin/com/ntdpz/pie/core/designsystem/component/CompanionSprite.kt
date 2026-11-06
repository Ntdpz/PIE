package com.ntdpz.pie.core.designsystem.component

import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ntdpz.pie.core.designsystem.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed interface SpriteImage {
    data object Loading : SpriteImage
    data class Ready(val normal: ImageBitmap, val reactions: ImageBitmap) : SpriteImage
    data object Failed : SpriteImage
}

private val LocalSpriteImage = staticCompositionLocalOf<SpriteImage?> { null }

/** Decode one atlas pair for a screen containing several previews. */
@Composable
fun CompanionSpriteGroup(content: @Composable () -> Unit) {
    val image by rememberSpriteImage()
    CompositionLocalProvider(LocalSpriteImage provides image, content = content)
}

@Composable
private fun rememberSpriteImage(): State<SpriteImage> {
    val resources = LocalContext.current.resources
    return produceState<SpriteImage>(SpriteImage.Loading, resources) {
        value = try {
            withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply { inScaled = false }
                SpriteImage.Ready(
                    BitmapFactory.decodeResource(resources, R.drawable.pie_companion, options).asImageBitmap(),
                    BitmapFactory.decodeResource(resources, R.drawable.pie_reactions, options).asImageBitmap(),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SpriteImage.Failed
        }
    }
}

/** Visual playback only. Feature code decides which animation a real event selects. */
@Composable
fun CompanionSprite(
    animation: CompanionAnimation,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    durationScale: Float = 1f,
    replayKey: Long = 0,
    characterScale: Float = 1f,
) {
    val image = LocalSpriteImage.current ?: rememberSpriteImage().value
    var frame by remember(animation, replayKey) { mutableIntStateOf(0) }
    val animated = motionEnabled && durationScale > 0f
    LaunchedEffect(animation, animated, durationScale, replayKey) {
        if (!animated) {
            frame = animation.stillFrame
            return@LaunchedEffect
        }
        do {
            for (index in 0 until 8) {
                frame = index
                delay((animation.frameDuration(index) * durationScale).toLong().coerceAtLeast(1))
            }
        } while (animation.loops)
    }
    val breathing = if (animated && animation == CompanionAnimation.Idle) {
        val transition = rememberInfiniteTransition(label = "PIE breathing")
        transition.animateFloat(
            1f, 0.985f,
            infiniteRepeatable(tween(2_000), RepeatMode.Reverse),
            label = "PIE breath height",
        ).value
    } else 1f
    when (val loaded = image) {
        SpriteImage.Loading -> CircularProgressIndicator(modifier)
        SpriteImage.Failed -> Text("เปิดภาพ PIE ไม่สำเร็จ", modifier)
        is SpriteImage.Ready -> Canvas(
            modifier.graphicsLayer {
                scaleY = breathing
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        ) {
            val scale = minOf(size.width, size.height) / 256f * characterScale
            val drawWidth = (animation.sourceWidth * scale).toInt()
            val drawHeight = (animation.sourceHeight * scale).toInt()
            drawImage(
                image = if (animation.reactionAtlas) loaded.reactions else loaded.normal,
                srcOffset = IntOffset(frame * animation.sourceWidth, animation.row * animation.sourceHeight),
                srcSize = IntSize(animation.sourceWidth, animation.sourceHeight),
                dstOffset = IntOffset((size.width.toInt() - drawWidth) / 2, size.height.toInt() - drawHeight),
                dstSize = IntSize(drawWidth, drawHeight),
                filterQuality = FilterQuality.None,
            )
        }
    }
}
