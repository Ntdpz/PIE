package com.ntdpz.pie.feature.history

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.video.VideoFrameDecoder

@Composable
internal fun rememberMemoryImageLoader(): ImageLoader {
    val context = LocalContext.current.applicationContext
    val loader = remember(context) {
        ImageLoader.Builder(context).components { add(VideoFrameDecoder.Factory()) }
            .memoryCache { coil3.memory.MemoryCache.Builder().maxSizePercent(context, 0.12).build() }.build()
    }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    return loader
}
