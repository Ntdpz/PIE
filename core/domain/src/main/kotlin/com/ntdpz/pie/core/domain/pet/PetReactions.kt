package com.ntdpz.pie.core.domain.pet

import kotlin.random.Random

enum class PetReaction(val durationMillis: Long) {
    Heart(1_440), Wink(800), EarsUp(560), HeadTilt(880), Nod(640),
    Shy(1_120), Cheer(1_120), Stretch(1_440), Peekaboo(1_040),
}

data class PetReactionSelection(val action: PetReaction, val sequence: Long, val startedAt: Long) {
    fun isActive(now: Long): Boolean = now >= startedAt && now - startedAt < action.durationMillis
}

/** Local interaction policy. Never queues clips or uses diary contents/touch coordinates. */
class PetReactions(private val randomIndex: (Int) -> Int = Random.Default::nextInt) {
    private var lastTouch: Long? = null
    private var quietSince = 0L
    private var stretched = false
    private var previous: PetReaction? = null
    private var sequence = 0L
    var selection: PetReactionSelection? = null
        private set

    fun reset(now: Long) {
        lastTouch = null
        quietSince = now
        stretched = false
        selection = null
    }

    fun touch(now: Long): PetReactionSelection {
        val rapid = lastTouch?.let { now >= it && now - it <= 350 } == true
        val pool = if (rapid) fast else regular
        val available = pool.filter { it != previous }
        lastTouch = now
        quietSince = now
        stretched = false
        return select(available[randomIndex(available.size)], now)
    }

    fun quiet(now: Long): PetReactionSelection? {
        if (stretched || now < quietSince || selection?.isActive(now) == true) return null
        if (now - quietSince !in 20_000 until PetBehavior.RestAfterMillis) return null
        stretched = true
        return select(PetReaction.Stretch, now)
    }

    private fun select(action: PetReaction, now: Long): PetReactionSelection {
        previous = action
        return PetReactionSelection(action, ++sequence, now).also { selection = it }
    }

    private companion object {
        val fast = listOf(PetReaction.Wink, PetReaction.EarsUp, PetReaction.Nod)
        val regular = listOf(
            PetReaction.Wink, PetReaction.EarsUp, PetReaction.Nod, PetReaction.HeadTilt,
            PetReaction.Wink, PetReaction.EarsUp, PetReaction.Nod, PetReaction.HeadTilt,
            PetReaction.Shy, PetReaction.Peekaboo, PetReaction.Cheer, PetReaction.Heart,
        )
    }
}
