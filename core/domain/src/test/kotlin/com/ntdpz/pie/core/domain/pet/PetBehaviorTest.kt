package com.ntdpz.pie.core.domain.pet

import com.ntdpz.pie.core.domain.diary.SaveState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBehaviorTest {
    private val date = LocalDate.of(2026, 11, 5)

    @Test
    fun autosavesDoNotCelebrateUntilTheVisitEndsAndNeverRepeat() {
        val policy = PetSaveCelebration()
        policy.begin(date, mapOf(date to SaveState.Saved(3)))
        for (revision in 4L..30L) {
            assertFalse(policy.observe(mapOf(date to SaveState.Saving(revision))))
            assertFalse(policy.observe(mapOf(date to SaveState.Saved(revision))))
        }
        val saved = mapOf(date to SaveState.Saved(30))
        assertTrue(policy.finish(date, saved))
        assertFalse(policy.observe(saved))
        assertFalse(policy.finish(date, saved))
        policy.begin(date, saved)
        assertFalse(policy.finish(date, saved))
    }

    @Test
    fun failedSaveAndAnotherDatesSaveCannotCelebrateThePendingVisit() {
        val policy = PetSaveCelebration()
        policy.begin(date, emptyMap())
        assertFalse(policy.finish(date, mapOf(date to SaveState.Saving(1))))
        assertFalse(policy.observe(mapOf(date to SaveState.Failed(1))))
        assertFalse(policy.observe(mapOf(date.plusDays(1) to SaveState.Saved(20))))
        assertTrue(policy.observe(mapOf(date to SaveState.Saved(2))))
        assertFalse(policy.observe(mapOf(date to SaveState.Saved(2))))
    }

    @Test
    fun pendingSaveKeepsItsSelectedDateAcrossMidnight() {
        val policy = PetSaveCelebration()
        policy.begin(date, emptyMap())
        assertFalse(policy.finish(date, mapOf(date to SaveState.Saving(1))))
        assertFalse(policy.observe(mapOf(date.plusDays(1) to SaveState.Saved(2))))
        assertTrue(policy.observe(mapOf(date to SaveState.Saved(1))))
    }

    @Test
    fun backgroundingAndStartingAnotherVisitDiscardPendingCelebrations() {
        val policy = PetSaveCelebration()
        policy.begin(date, emptyMap())
        policy.finish(date, mapOf(date to SaveState.Saving(1)))
        policy.dismiss()
        assertFalse(policy.observe(mapOf(date to SaveState.Saved(1))))
        policy.begin(date, emptyMap())
        policy.finish(date, mapOf(date to SaveState.Saving(2)))
        policy.begin(date.plusDays(1), emptyMap())
        assertFalse(policy.observe(mapOf(date to SaveState.Saved(2))))
    }

    @Test
    fun openingAnAlreadyPendingDraftDoesNotClaimANewEdit() {
        val policy = PetSaveCelebration()
        policy.begin(date, mapOf(date to SaveState.Saving(8)))
        assertFalse(policy.finish(date, mapOf(date to SaveState.Saved(8))))
    }

    @Test
    fun mediaOnlyCommitCelebratesButCannotHideAFailedOrPendingTextSave() {
        val policy = PetSaveCelebration()
        policy.begin(date, emptyMap())
        policy.mediaCommitted(date.plusDays(1))
        assertFalse(policy.finish(date, emptyMap()))
        policy.begin(date, emptyMap())
        policy.mediaCommitted(date)
        assertTrue(policy.finish(date, emptyMap()))
        policy.begin(date, emptyMap())
        policy.mediaCommitted(date)
        assertFalse(policy.finish(date, mapOf(date to SaveState.Failed(1))))
        assertFalse(policy.observe(mapOf(date to SaveState.Saving(2))))
        assertTrue(policy.observe(mapOf(date to SaveState.Saved(2))))
    }

    @Test
    fun transientReactionsExpireRestWaitsAndClockRollbackIsSafe() {
        assertEquals(PetPose.Greeting, PetBehavior.pose(1_000, 1_000, PetPose.Greeting, 1_000))
        assertEquals(PetPose.Idle, PetBehavior.pose(3_400, 1_000, PetPose.Greeting, 1_000))
        assertEquals(PetPose.Idle, PetBehavior.pose(45_999, 1_000, null, 1_000))
        assertEquals(PetPose.Rest, PetBehavior.pose(46_000, 1_000, null, 1_000))
        assertEquals(PetPose.Idle, PetBehavior.pose(0, 1_000, PetPose.Saved, 1_000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun positionsRejectNonFiniteCoordinates() {
        PetPosition(Float.NaN, 0f)
    }
}
