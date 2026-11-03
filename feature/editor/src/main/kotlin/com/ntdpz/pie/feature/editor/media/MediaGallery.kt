package com.ntdpz.pie.feature.editor.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.ImageLoader
import coil3.video.VideoFrameDecoder
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.domain.media.DiaryMedia
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
internal fun MediaGallery(media: List<DiaryMedia>, onDelete: (String) -> Unit) {
    var selected by remember { mutableStateOf<DiaryMedia?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { selected = null }
    val context = LocalContext.current
    val imageLoader = remember(context) { ImageLoader.Builder(context).components { add(VideoFrameDecoder.Factory()) }.build() }
    DisposableEffect(imageLoader) { onDispose { imageLoader.shutdown() } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(media, key = { it.id }) { item ->
                Surface(
                    modifier = Modifier.width(166.dp).height(142.dp).clip(RoundedCornerShape(14.dp))
                        .clickable { selected = item },
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box {
                        AsyncImage(model = item.uri, imageLoader = imageLoader, contentDescription = if (item.isVideo) "วิดีโอของวันนี้" else "ภาพของวันนี้", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        if (item.isVideo) Surface(Modifier.align(Alignment.BottomStart).padding(10.dp), color = Color.Black.copy(alpha = .55f), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PieIcon(PieGlyph.Play, "เปิดวิดีโอ", color = Color.White, size = 18.dp)
                                Text("วิดีโอ", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
    selected?.let { item -> MediaViewer(item, { selected = null }, { onDelete(item.id); selected = null }) }
}

@Composable
private fun MediaViewer(item: DiaryMedia, onClose: () -> Unit, onDelete: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.systemBarsPadding()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onClose) { PieIcon(PieGlyph.Close, "ปิดสื่อ", color = Color.White) }
                    IconButton(onClick = { confirmDelete = true }) { PieIcon(PieGlyph.Trash, "ลบสื่อ", color = Color.White) }
                }
                if (item.isVideo) LocalVideoPlayer(item.uri, Modifier.weight(1f))
                else AsyncImage(item.uri, "ภาพของวันนี้", Modifier.fillMaxWidth().weight(1f), contentScale = ContentScale.Fit)
            }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("ลบสื่อนี้?") },
        text = { Text("ลบเฉพาะสำเนาใน PIE ไฟล์ต้นฉบับในแกลเลอรีจะยังอยู่") },
        confirmButton = { TextButton(onClick = onDelete) { Text("ลบ") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("ยกเลิก") } },
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun LocalVideoPlayer(uri: String, modifier: Modifier) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(uri)); prepare() }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = modifier.fillMaxWidth())
}
