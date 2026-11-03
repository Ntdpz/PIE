package com.ntdpz.pie.feature.history

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ntdpz.pie.core.domain.media.DiaryMedia

@Composable
internal fun MemoryAttachment(item: DiaryMedia, loader: ImageLoader, active: Boolean, onZoom: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    if (item.isVideo) MemoryVideo(item.uri, loader, active, modifier)
    else MemoryPhoto(item.uri, loader, active, onZoom, modifier)
}

@Composable
private fun MemoryPhoto(uri: String, loader: ImageLoader, active: Boolean, onZoom: (Boolean) -> Unit, modifier: Modifier) {
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    var failed by remember(uri) { mutableStateOf(false) }
    var retry by remember(uri) { mutableIntStateOf(0) }
    LaunchedEffect(active) { if (!active) { scale = 1f; offset = Offset.Zero } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { scale = 1f; offset = Offset.Zero; onZoom(false) }
    LaunchedEffect(scale, active) { if (active) onZoom(scale > 1.01f) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 4f)
        offset = if (scale <= 1f) Offset.Zero else Offset((offset.x + pan.x).coerceIn(-800f, 800f), (offset.y + pan.y).coerceIn(-1200f, 1200f))
    }
    val context = LocalContext.current
    val request = remember(uri, retry, context) { ImageRequest.Builder(context).data(uri).size(1600, 1600).memoryCacheKey("$uri:$retry").build() }
    Box(modifier) {
        AsyncImage(request, "ภาพความทรงจำ", imageLoader = loader, contentScale = ContentScale.Fit,
            onError = { failed = true }, onSuccess = { failed = false },
            modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                .transformable(transform, canPan = { scale > 1.01f })
                .pointerInput(uri) { detectTapGestures(onDoubleTap = { scale = if (scale > 1f) 1f else 2f; offset = Offset.Zero }) },
        )
        if (scale > 1.01f) TextButton(onClick = { scale = 1f; offset = Offset.Zero }, Modifier.align(Alignment.BottomEnd).padding(12.dp)) { Text("คืนขนาดภาพ") }
        if (failed) TextButton(onClick = { retry++; failed = false }, Modifier.align(Alignment.Center)) { Text("เปิดภาพไม่สำเร็จ · ลองอีกครั้ง") }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun MemoryVideo(uri: String, loader: ImageLoader, active: Boolean, modifier: Modifier) {
    var playing by remember(uri) { mutableStateOf(false) }
    var sound by remember(uri) { mutableStateOf(false) }
    LaunchedEffect(active) { if (!active) { playing = false; sound = false } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { playing = false; sound = false }
    val context = LocalContext.current
    Box(modifier) {
        if (!playing || !active) {
            AsyncImage(uri, "วิดีโอความทรงจำ", imageLoader = loader, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            FilledTonalButton(onClick = { playing = true }, Modifier.align(Alignment.Center)) { Text("เล่นวิดีโอ") }
        } else {
            val player = remember(uri) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare(); playWhenReady = true; volume = 0f } }
            DisposableEffect(player) { onDispose { player.release() } }
            LaunchedEffect(sound) { player.volume = if (sound) 1f else 0f }
            AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
            FilledTonalButton(onClick = { sound = !sound }, Modifier.align(Alignment.BottomEnd).padding(12.dp)) { Text(if (sound) "ปิดเสียง" else "เปิดเสียง") }
        }
    }
}
