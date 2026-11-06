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
    fun hysteresisPreventsFlickerUntilLeavingTheEdge() {
        assertEquals(PetEdge.Center, PetEdges.at(PetPosition(.1f, .5f)))
        assertEquals(PetEdge.Left, PetEdges.at(PetPosition(.1f, .5f), PetEdge.Left))
        assertEquals(PetEdge.Center, PetEdges.at(PetPosition(.15f, .5f), PetEdge.Left))
        assertEquals(PetEdge.TopRight, PetEdges.at(PetPosition(.9f, .1f), PetEdge.TopRight))
    }

    @Test
    fun snappingOnlyChangesCoordinatesNearAnEdge() {
        assertEquals(PetPosition(0f, 1f), PetEdges.snap(PetPosition(.03f, .96f)))
        assertEquals(PetPosition(1f, .6f), PetEdges.snap(PetPosition(.95f, .6f)))
        assertEquals(PetPosition(.3f, .7f), PetEdges.snap(PetPosition(.3f, .7f)))
    }
}
