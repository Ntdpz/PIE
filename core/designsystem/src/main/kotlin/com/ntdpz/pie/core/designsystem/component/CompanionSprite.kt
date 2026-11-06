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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

enum class CompanionAnimation { Idle, Tap, Greeting, Saved, Rest }

private sealed interface SpriteImage {
    data object Loading : SpriteImage
    data class Ready(val image: ImageBitmap) : SpriteImage
    data object Failed : SpriteImage
}

/** Visual playback only. Feature code decides which animation a real event selects. */
@Composable
fun CompanionSprite(
    animation: CompanionAnimation,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
    durationScale: Float = 1f,
) {
    val resources = LocalContext.current.resources
    val image by produceState<SpriteImage>(SpriteImage.Loading, resources) {
        value = try {
            withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply { inScaled = false }
                SpriteImage.Ready(BitmapFactory.decodeResource(resources, R.drawable.pie_companion, options).asImageBitmap())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SpriteImage.Failed
        }
    }
    var frame by remember(animation) { mutableIntStateOf(0) }
    val animated = motionEnabled && durationScale > 0f
    LaunchedEffect(animation, animated, durationScale) {
        if (!animated) {
            frame = when (animation) {
                CompanionAnimation.Tap -> 5
                CompanionAnimation.Greeting, CompanionAnimation.Saved -> 3
                else -> 0
            }
            return@LaunchedEffect
        }
        val durations = when (animation) {
            CompanionAnimation.Idle -> intArrayOf(1_000, 1_000, 800, 120, 1_000, 800, 200, 1_000)
            CompanionAnimation.Rest -> IntArray(8) { 1_500 }
            else -> IntArray(8) { 180 }
        }
        do {
            for (index in durations.indices) {
                frame = index
                delay((durations[index] * durationScale).toLong().coerceAtLeast(1))
            }
        } while (animation == CompanionAnimation.Idle || animation == CompanionAnimation.Rest)
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
            drawImage(
                image = loaded.image,
                srcOffset = IntOffset(frame * 192, animation.ordinal * 208),
                srcSize = IntSize(192, 208),
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None,
            )
        }
    }
}
