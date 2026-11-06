package com.ntdpz.pie.core.designsystem.component

/** Atlas geometry and playback only; features decide when an action is appropriate. */
enum class CompanionAnimation(
    val row: Int,
    val reactionAtlas: Boolean = false,
    private val frameMillis: Int = 180,
    val stillFrame: Int = 3,
) {
    Idle(0, stillFrame = 0), Tap(1, stillFrame = 5), Greeting(2), Saved(3), Rest(4, stillFrame = 0),
    Wink(0, true, 100), EarsUp(1, true, 70), HeadTilt(2, true, 110), Nod(3, true, 80),
    Shy(4, true, 140), Cheer(5, true, 140), Stretch(6, true, 180), Peekaboo(7, true, 130);

    val loops: Boolean get() = this == Idle || this == Rest
    val sourceWidth: Int get() = if (reactionAtlas) 256 else 192
    val sourceHeight: Int get() = if (reactionAtlas) 256 else 208

    fun frameDuration(index: Int): Int = when (this) {
        Idle -> idleDurations[index]
        Rest -> 1_500
        else -> frameMillis
    }

    private companion object {
        val idleDurations = intArrayOf(1_000, 1_000, 800, 120, 1_000, 800, 200, 1_000)
    }
}
