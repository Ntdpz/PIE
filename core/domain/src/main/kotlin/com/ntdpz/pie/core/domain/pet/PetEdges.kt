package com.ntdpz.pie.core.domain.pet

enum class PetEdge(val horizontal: Int, val vertical: Int) {
    Center(0, 0), TopLeft(-1, -1), Top(0, -1), TopRight(1, -1),
    Left(-1, 0), Right(1, 0), BottomLeft(-1, 1), Bottom(0, 1), BottomRight(1, 1),
}

/**
 * Relative draggable-area coordinates; no screen size, Android context or persistence.
 * Stateless on purpose: the in-app companion and the overlay share one saved position, so
 * both must derive the same edge from it without remembering their own previous edge.
 */
object PetEdges {
    private const val Snap = 0.08f

    fun at(position: PetPosition): PetEdge {
        val horizontal = axis(position.x)
        val vertical = axis(position.y)
        return PetEdge.entries.first { it.horizontal == horizontal && it.vertical == vertical }
    }

    /** Every snapped position resolves through [at] to the edge it was snapped to. */
    fun snap(position: PetPosition): PetPosition {
        val edge = at(position)
        return PetPosition(
            when (edge.horizontal) { -1 -> 0f; 1 -> 1f; else -> position.x },
            when (edge.vertical) { -1 -> 0f; 1 -> 1f; else -> position.y },
        )
    }

    private fun axis(value: Float): Int = when {
        value <= Snap -> -1
        value >= 1f - Snap -> 1
        else -> 0
    }
}
