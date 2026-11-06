package com.ntdpz.pie.core.domain.pet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetReactionsTest {
    @Test
    fun repeatedTouchesNeverSelectSameActionAndNeverQueue() {
        val reactions = PetReactions { 0 }
        var previous: PetReaction? = null
        repeat(100) { index ->
            val selected = reactions.touch(index * 100L)
            assertNotEquals(previous, selected.action)
            assertEquals(index + 1L, selected.sequence)
            assertEquals(selected, reactions.selection)
            previous = selected.action
        }
    }

    @Test
    fun rapidTouchesUseShortActionsAndResumeNormalChoiceAfterGap() {
        val reactions = PetReactions { it - 1 }
        assertEquals(PetReaction.Heart, reactions.touch(0).action)
        repeat(10) { index ->
            assertTrue(reactions.touch(100L * (index + 1)).action in setOf(PetReaction.Wink, PetReaction.EarsUp, PetReaction.Nod))
        }
        assertEquals(PetReaction.Heart, reactions.touch(5_000).action)
    }

    @Test
    fun stretchOccursOncePerQuietIntervalAndTouchInterruptsIt() {
        val reactions = PetReactions { 0 }
        reactions.reset(1_000)
        assertNull(reactions.quiet(20_999))
        val stretch = reactions.quiet(21_000)!!
        assertEquals(PetReaction.Stretch, stretch.action)
        assertNull(reactions.quiet(23_000))
        assertNotEquals(PetReaction.Stretch, reactions.touch(23_001).action)
        assertNull(reactions.quiet(43_000))
        assertEquals(PetReaction.Stretch, reactions.quiet(43_001)!!.action)
    }

    @Test
    fun resetDiscardsPlaybackAndDoesNotStretchAfterRestOrClockRollback() {
        val reactions = PetReactions { 0 }
        val reaction = reactions.touch(1_000)
        assertTrue(reaction.isActive(1_000))
        assertFalse(reaction.isActive(999))
        assertFalse(reaction.isActive(1_000 + reaction.action.durationMillis))
        reactions.reset(2_000)
        assertNull(reactions.selection)
        assertNull(reactions.quiet(1_000))
        assertNull(reactions.quiet(47_000))
    }

    @Test
    fun allTouchActionsAreReachableButStretchIsNeverRandomlySelected() {
        val found = (0 until 12).map { index -> PetReactions { index % it }.touch(0).action }.toSet()
        assertEquals(PetReaction.entries.toSet() - PetReaction.Stretch, found)
    }
}
