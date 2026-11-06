package com.ntdpz.pie.core.domain.pet

import kotlinx.coroutines.flow.Flow

data class PetPosition(val x: Float = 1f, val y: Float = 0.72f) {
    init {
        require(x.isFinite() && y.isFinite() && x in 0f..1f && y in 0f..1f)
    }
}

data class PetPreferences(
    val enabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val position: PetPosition = PetPosition(),
)

interface PetRepository {
    val preferences: Flow<PetPreferences>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun setReducedMotion(enabled: Boolean)
    suspend fun setPosition(position: PetPosition)
}

enum class PetPose { Idle, Tap, Greeting, Saved, Rest }

object PetBehavior {
    const val RestAfterMillis = 45_000L
    const val ReactionMillis = 2_400L

    fun pose(now: Long, lastInteraction: Long, reaction: PetPose?, reactionAt: Long): PetPose {
        if (now < lastInteraction || now < reactionAt) return PetPose.Idle
        if (reaction != null && now - reactionAt < ReactionMillis) return reaction
        return if (now - lastInteraction >= RestAfterMillis) PetPose.Rest else PetPose.Idle
    }
}
