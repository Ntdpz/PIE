package com.ntdpz.pie.core.domain.pet

enum class PetEdge(val horizontal: Int, val vertical: Int) {
    Center(0, 0), TopLeft(-1, -1), Top(0, -1), TopRight(1, -1),
    Left(-1, 0), Right(1, 0), BottomLeft(-1, 1), Bottom(0, 1), BottomRight(1, 1),
}

/** Relative draggable-area coordinates; no screen size, Android context or persistence. */
object PetEdges {
    private const val Enter = 0.08f
    private const val Leave = 0.14f

    fun at(position: PetPosition, previous: PetEdge = PetEdge.Center): PetEdge {
        val horizontal = axis(position.x, previous.horizontal)
        val vertical = axis(position.y, previous.vertical)
        return PetEdge.entries.first { it.horizontal == horizontal && it.vertical == vertical }
    }

    fun snap(position: PetPosition): PetPosition {
        val edge = at(position)
        return PetPosition(
            when (edge.horizontal) { -1 -> 0f; 1 -> 1f; else -> position.x },
            when (edge.vertical) { -1 -> 0f; 1 -> 1f; else -> position.y },
        )
    }

    private fun axis(value: Float, previous: Int): Int = when {
        previous == -1 && value <= Leave -> -1
        previous == 1 && value >= 1f - Leave -> 1
        value <= Enter -> -1
        value >= 1f - Enter -> 1
        else -> 0
    }
}
