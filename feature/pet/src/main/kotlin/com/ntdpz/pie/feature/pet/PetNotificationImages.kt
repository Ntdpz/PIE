package com.ntdpz.pie.feature.pet

import android.content.res.Resources
import com.ntdpz.pie.core.designsystem.component.CompanionNotificationImages
import com.ntdpz.pie.core.domain.pet.PetReaction
import com.ntdpz.pie.core.designsystem.theme.CompanionColors
import androidx.compose.ui.graphics.toArgb

class PetNotificationImages(resources: Resources) {
    private val artwork = CompanionNotificationImages(resources)
    val smallIcon: Int get() = artwork.smallIcon
    val color: Int get() = CompanionColors.Rose.toArgb()
    fun head() = artwork.head()
    fun forReaction(reaction: PetReaction) = artwork.pose(reaction.animation())
}
