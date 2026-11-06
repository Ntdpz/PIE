package com.ntdpz.pie

import com.ntdpz.pie.feature.pet.overlay.PetOverlayService

class PiePetOverlayService : PetOverlayService() {
    private val container get() = (application as PieApplication).container
    override val petRepository get() = container.petRepository
    override val preferenceWriteScope get() = container.applicationScope
}
