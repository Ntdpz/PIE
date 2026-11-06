package com.ntdpz.pie.core.designsystem.motion

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberSystemAnimationScale(): Float {
    val resolver = LocalContext.current.contentResolver
    fun readScale(): Float = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        .takeIf { it.isFinite() && it >= 0f } ?: 1f
    var scale by remember(resolver) { mutableFloatStateOf(readScale()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = readScale() }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        scale = readScale()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale
}
