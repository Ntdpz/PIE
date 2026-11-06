package com.ntdpz.pie.feature.pet

import com.ntdpz.pie.core.designsystem.component.CompanionAnimation
import com.ntdpz.pie.core.domain.pet.PetReaction

internal fun PetReaction.animation(): CompanionAnimation = when (this) {
    PetReaction.Heart -> CompanionAnimation.Tap
    PetReaction.Wink -> CompanionAnimation.Wink
    PetReaction.EarsUp -> CompanionAnimation.EarsUp
    PetReaction.HeadTilt -> CompanionAnimation.HeadTilt
    PetReaction.Nod -> CompanionAnimation.Nod
    PetReaction.Shy -> CompanionAnimation.Shy
    PetReaction.Cheer -> CompanionAnimation.Cheer
    PetReaction.Stretch -> CompanionAnimation.Stretch
    PetReaction.Peekaboo -> CompanionAnimation.Peekaboo
}

internal fun PetReaction.label(): String = when (this) {
    PetReaction.Heart -> "ส่งหัวใจ"
    PetReaction.Wink -> "ขยิบตา"
    PetReaction.EarsUp -> "หูตั้ง"
    PetReaction.HeadTilt -> "เอียงคอ"
    PetReaction.Nod -> "พยักหน้า"
    PetReaction.Shy -> "เขิน"
    PetReaction.Cheer -> "ดีใจ"
    PetReaction.Stretch -> "ยืดเส้น"
    PetReaction.Peekaboo -> "จ๊ะเอ๋"
}
