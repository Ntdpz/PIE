package com.ntdpz.pie.core.designsystem.component

/**
 * Visual atlas cells only; feature/domain code decides when an edge pose is appropriate.
 * [horizontal]/[vertical] point toward the screen edge the pose clings to (-1, 0 or 1).
 */
enum class CompanionEdge(val column: Int, val anchorY: Float, val horizontal: Int, val vertical: Int) {
    Center(-1, 1f, 0, 0),
    TopLeft(0, 0f, -1, -1), Top(1, 0f, 0, -1), TopRight(2, 0f, 1, -1),
    Left(3, .5f, -1, 0), Right(4, .5f, 1, 0),
    BottomLeft(5, 1f, -1, 1), Bottom(6, 1f, 0, 1), BottomRight(7, 1f, 1, 1),
}
