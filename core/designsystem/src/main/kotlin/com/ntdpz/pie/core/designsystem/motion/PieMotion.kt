package com.ntdpz.pie.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing

object PieMotion {
    const val QuickMillis = 140
    const val PageMillis = 320
    const val GentleMillis = 280
    const val RevealMillis = 420
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
