package com.ntdpz.pie.core.designsystem.component

/** Visual atlas cells only; feature/domain code decides when an edge pose is appropriate. */
enum class CompanionEdge(val column: Int, val anchorY: Float) {
    Center(-1, 1f), TopLeft(0, 0f), Top(1, 0f), TopRight(2, 0f),
    Left(3, .5f), Right(4, .5f), BottomLeft(5, 1f), Bottom(6, 1f), BottomRight(7, 1f),
}
