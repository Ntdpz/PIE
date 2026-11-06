package com.ntdpz.pie.core.designsystem.component

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.draw.clipToBounds
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

/** The edge atlas is decoded only once a sprite actually needs an edge pose. */
private class EdgeAtlas(private val resources: Resources) {
    var image by mutableStateOf<ImageBitmap?>(null)
        private set
    var failed by mutableStateOf(false)
        private set
    private var loading = false

    /** Main-thread only, so concurrent sprites in one group share a single decode. */
    suspend fun load() {
        if (loading || image != null || failed) return
        loading = true
        try {
            image = withContext(Dispatchers.IO) {
                val options = BitmapFactory.Options().apply { inScaled = false }
                BitmapFactory.decodeResource(resources, R.drawable.pie_edges, options).asImageBitmap()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failed = true
        } finally {
            loading = false
        }
    }
}

private val LocalSpriteImage = staticCompositionLocalOf<SpriteImage?> { null }
private val LocalEdgeAtlas = staticCompositionLocalOf<EdgeAtlas?> { null }

/** Decode one atlas set for a screen containing several previews. */
@Composable
fun CompanionSpriteGroup(content: @Composable () -> Unit) {
    val image by rememberSpriteImage()
    val resources = LocalContext.current.resources
    val edges = remember(resources) { EdgeAtlas(resources) }
    CompositionLocalProvider(LocalSpriteImage provides image, LocalEdgeAtlas provides edges, content = content)
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
    edge: CompanionEdge = CompanionEdge.Center,
) {
    val image = LocalSpriteImage.current ?: rememberSpriteImage().value
    val resources = LocalContext.current.resources
    val edgeAtlas = LocalEdgeAtlas.current ?: remember(resources) { EdgeAtlas(resources) }
    var frame by remember(animation, replayKey) { mutableIntStateOf(0) }
    val animated = motionEnabled && durationScale > 0f
    val wantsEdge = edge != CompanionEdge.Center
    LaunchedEffect(edgeAtlas, wantsEdge) { if (wantsEdge) edgeAtlas.load() }
    val edgeImage = edgeAtlas.image.takeIf { wantsEdge }
    // Created with the new edge so its first frame is already transparent; no one-frame flash.
    val entry = remember(edge) { Animatable(if (wantsEdge && animated) 0f else 1f) }
    // Keyed on the edge (via entry), not on motion: toggling motion finishes the fade, never replays it.
    LaunchedEffect(entry, edgeImage != null, animated) {
        // Hold the fade until the atlas arrives; the normal pose is drawn meanwhile.
        if (edgeImage == null && wantsEdge && !edgeAtlas.failed) return@LaunchedEffect
        if (animated) entry.animateTo(1f, tween((180 * durationScale).toInt().coerceAtLeast(1)))
        else entry.snapTo(1f)
    }
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
            modifier.clipToBounds().graphicsLayer {
                scaleY = breathing
                transformOrigin = TransformOrigin(0.5f, edge.anchorY)
            },
        ) {
            val scale = minOf(size.width, size.height) / 256f * characterScale
            val edges = edgeImage
            val peeking = edges != null
            val sourceWidth = if (peeking) 256 else animation.sourceWidth
            val sourceHeight = if (peeking) 256 else animation.sourceHeight
            val drawWidth = (sourceWidth * scale).toInt()
            val drawHeight = (sourceHeight * scale).toInt()
            val horizontal = if (peeking) edge.horizontal else 0
            val vertical = if (peeking) edge.vertical else 0
            val shift = (1f - entry.value) * minOf(size.width, size.height) * .06f
            drawImage(
                image = if (edges != null) edges else if (animation.reactionAtlas) loaded.reactions else loaded.normal,
                srcOffset = if (peeking) IntOffset(edge.column * 256, 0) else IntOffset(frame * sourceWidth, animation.row * sourceHeight),
                srcSize = IntSize(sourceWidth, sourceHeight),
                dstOffset = IntOffset(
                    (size.width.toInt() - drawWidth) / 2 + (horizontal * shift).toInt(),
                    size.height.toInt() - drawHeight + (vertical * shift).toInt(),
                ),
                dstSize = IntSize(drawWidth, drawHeight),
                alpha = if (peeking) entry.value else 1f,
                filterQuality = FilterQuality.None,
            )
        }
    }
}
