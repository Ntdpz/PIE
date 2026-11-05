package com.ntdpz.pie.core.domain.diary

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryWriterTest {
    private val date = LocalDate.of(2026, 11, 2)
    private val clock = Clock.fixed(Instant.parse("2026-11-02T16:59:00Z"), ZoneOffset.UTC)

    @Test
    fun rapidEditsPersistInOrderAndLastTextWins() = runTest {
        val repository = RecordingRepository()
        val writer = DiaryWriter(repository, backgroundScope, clock)
        listOf("a", "ab", "abc").forEach { writer.submit(date, it) }
        runCurrent()
        assertEquals(listOf("a", "ab", "abc"), repository.writes.map { it.text })
        assertTrue(writer.states.value[date] is SaveState.Saved)
        assertNull(writer.pendingText(date))
    }

    @Test
    fun olderCompletionDoesNotMarkNewerDraftSaved() = runTest {
        val firstWrite = CompletableDeferred<Unit>()
        val latestWrite = CompletableDeferred<Unit>()
        val repository = RecordingRepository(beforeWrite = { entry ->
            if (entry.text == "first") firstWrite.await()
            if (entry.text == "latest") latestWrite.await()
        })
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "first")
        runCurrent()
        writer.submit(date, "latest")
        assertTrue(writer.states.value[date] is SaveState.Saving)
        assertEquals("latest", writer.pendingText(date))
        firstWrite.complete(Unit)
        runCurrent()
        assertTrue(writer.states.value[date] is SaveState.Saving)
        assertEquals("latest", writer.pendingText(date))
        latestWrite.complete(Unit)
        runCurrent()
        assertEquals("latest", repository.writes.last().text)
        assertTrue(writer.states.value[date] is SaveState.Saved)
    }

    @Test
    fun formattingSurvivesFailedDraftAndRetryInWriteOrder() = runTest {
        var fail = true
        val repository = RecordingRepository(beforeWrite = { if (fail) throw IOException("disk") })
        val writer = DiaryWriter(repository, backgroundScope, clock)
        val styles = listOf(TextFormatRange(0, 4, TextFormat.Bold))
        writer.submit(date, "text", styles)
        runCurrent()
        assertEquals(styles, writer.pendingEntry(date)?.formatting)
        fail = false
        writer.submit(date, "text", styles)
        runCurrent()
        assertEquals(styles, repository.writes.last().formatting)
        assertNull(writer.pendingEntry(date))
    }

    @Test
    fun writesKeepTheirSelectedDayAcrossMidnight() = runTest {
        val repository = RecordingRepository()
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "before midnight")
        writer.submit(date.plusDays(1), "next page")
        writer.submit(date, "continue original page")
        runCurrent()
        assertEquals(listOf(date, date.plusDays(1), date), repository.writes.map { it.date })
        assertEquals(clock.instant(), repository.writes.last().updatedAt)
    }

    @Test
    fun failurePreservesDraftAndNextSubmitCanRecover() = runTest {
        var fail = true
        val repository = RecordingRepository(beforeWrite = { if (fail) throw IOException("disk") })
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "keep me")
        runCurrent()
        assertTrue(writer.states.value[date] is SaveState.Failed)
        assertEquals("keep me", writer.pendingText(date))
        assertFalse(repository.writes.isNotEmpty())
        fail = false
        writer.submit(date, "keep me")
        runCurrent()
        assertEquals("keep me", repository.writes.single().text)
        assertTrue(writer.states.value[date] is SaveState.Saved)
    }

    @Test
    fun clearingTextDoesNotRestoreAnOlderDraft() = runTest {
        val repository = RecordingRepository()
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "old")
        writer.submit(date, "")
        assertEquals("", writer.pendingText(date))
        runCurrent()
        assertEquals("", repository.writes.last().text)
    }

    @Test
    fun deleteWaitsForAcceptedWritesAndDoesNotResurrectTheDay() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = RecordingRepository(beforeWrite = { gate.await() })
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "pending")
        runCurrent()
        val deletion = async { writer.delete(date) }
        runCurrent()
        assertFalse(deletion.isCompleted)
        gate.complete(Unit)
        runCurrent()
        assertTrue(deletion.await().isSuccess)
        assertTrue(repository.writes.isEmpty())
        assertNull(writer.pendingText(date))
        assertNull(writer.states.value[date])
    }

    @Test
    fun backupBarrierRejectsAnUnpersistedDraft() = runTest {
        val repository = RecordingRepository(beforeWrite = { throw IOException("disk") })
        val writer = DiaryWriter(repository, backgroundScope, clock)
        writer.submit(date, "not saved")
        runCurrent()
        val barrier = async { runCatching { writer.awaitIdle() } }
        runCurrent()
        assertTrue(barrier.await().isFailure)
        assertEquals("not saved", writer.pendingText(date))
    }
}

private class RecordingRepository(
    private val beforeWrite: suspend (DiaryEntry) -> Unit = {},
) : DiaryRepository {
    val writes = mutableListOf<DiaryEntry>()
    override fun observeEntry(date: LocalDate): Flow<DiaryEntry?> = flowOf(null)
    override fun observeSummaries(): Flow<List<DiarySummary>> = flowOf(emptyList())
    override suspend fun readEntry(date: LocalDate): DiaryEntry? = null
    override suspend fun deleteEntry(date: LocalDate) { writes.removeAll { it.date == date } }
    override suspend fun saveEntry(entry: DiaryEntry) {
        beforeWrite(entry)
        writes += entry
    }
}
