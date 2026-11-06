package com.ntdpz.pie.core.domain.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetEdgesTest {
    @Test
    fun allEightEdgesAndTheCenterAreDistinct() {
        PetEdge.entries.forEach { edge ->
            val position = PetPosition((edge.horizontal + 1) / 2f, (edge.vertical + 1) / 2f)
            assertEquals(edge, PetEdges.at(position))
        }
    }

    @Test
    fun snappingOnlyChangesCoordinatesNearAnEdge() {
        assertEquals(PetPosition(0f, 1f), PetEdges.snap(PetPosition(.03f, .96f)))
        assertEquals(PetPosition(1f, .6f), PetEdges.snap(PetPosition(.95f, .6f)))
        assertEquals(PetPosition(.3f, .7f), PetEdges.snap(PetPosition(.3f, .7f)))
    }

    @Test
    fun aDropJustOutsideTheSnapZoneNeverShowsAnEdgePose() {
        val dropped = PetPosition(.11f, .5f)
        assertEquals(dropped, PetEdges.snap(dropped))
        assertEquals(PetEdge.Center, PetEdges.at(PetEdges.snap(dropped)))
    }

    @Test
    fun everySnappedPositionIsExactlyOnTheEdgeItShows() {
        val steps = (0..100).map { it / 100f }
        steps.forEach { x ->
            steps.forEach { y ->
                val snapped = PetEdges.snap(PetPosition(x, y))
                val edge = PetEdges.at(snapped)
                if (edge.horizontal != 0) assertEquals(if (edge.horizontal < 0) 0f else 1f, snapped.x)
                if (edge.vertical != 0) assertEquals(if (edge.vertical < 0) 0f else 1f, snapped.y)
                assertEquals(edge, PetEdges.at(PetPosition(x, y)))
            }
        }
    }
}
