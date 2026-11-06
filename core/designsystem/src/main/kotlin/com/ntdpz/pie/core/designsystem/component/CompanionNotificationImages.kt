package com.ntdpz.pie.core.designsystem.component

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import com.ntdpz.pie.core.designsystem.R

/** Exact approved raster assets. Notification layout and image size remain system-owned. */
class CompanionNotificationImages(private val resources: Resources) {
    val smallIcon: Int get() = R.drawable.ic_notification_pet_head

    fun head(): Bitmap = decode(R.drawable.pie_notification_head)

    fun pose(animation: CompanionAnimation): Bitmap = decode(
        when (animation) {
            CompanionAnimation.Wink -> R.drawable.pie_notice_wink
            CompanionAnimation.EarsUp -> R.drawable.pie_notice_ears_up
            CompanionAnimation.HeadTilt -> R.drawable.pie_notice_head_tilt
            CompanionAnimation.Nod -> R.drawable.pie_notice_nod
            CompanionAnimation.Shy -> R.drawable.pie_notice_shy
            CompanionAnimation.Cheer -> R.drawable.pie_notice_cheer
            CompanionAnimation.Stretch -> R.drawable.pie_notice_stretch
            CompanionAnimation.Peekaboo -> R.drawable.pie_notice_peekaboo
            CompanionAnimation.Tap -> R.drawable.pie_notice_heart
            else -> R.drawable.pie_notification_head
        },
    )

    private fun decode(@DrawableRes id: Int): Bitmap = requireNotNull(
        BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inScaled = false }),
    )
}
