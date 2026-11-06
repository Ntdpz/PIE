package com.ntdpz.pie

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout

/** Separate-process probe, enabled only in isolated device-test builds. */
class OverlayTouchProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var touches = 0
        val button = Button(this).apply {
            isAllCaps = false
            text = "Probe touches: 0"
            contentDescription = "overlay-touch-probe"
            setOnClickListener {
                touches++
                text = "Probe touches: $touches"
            }
        }
        setContentView(FrameLayout(this).apply {
            addView(button, FrameLayout.LayoutParams(600, 180, Gravity.CENTER))
        })
    }
}
