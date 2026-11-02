package com.ntdpz.pie.core.domain.diary

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

/**
 * App-scoped, serial write station. Leaving a screen does not cancel accepted writes.
 * A revision prevents an older completion from marking a newer draft as saved.
 */
class DiaryWriter(
    private val repository: DiaryRepository,
    scope: CoroutineScope,
    private val clock: Clock,
) {
    private data class Draft(val entry: DiaryEntry, val revision: Long)

    private val revisions = AtomicLong()
    private val drafts = Channel<Draft>(Channel.UNLIMITED)
    private val pendingDrafts = ConcurrentHashMap<LocalDate, Draft>()
    private val mutableStates = MutableStateFlow<Map<LocalDate, SaveState>>(emptyMap())
    val states = mutableStates.asStateFlow()

    init {
        scope.launch {
            for (draft in drafts) persist(draft)
        }
    }

    fun submit(date: LocalDate, text: String) {
        val revision = revisions.incrementAndGet()
        mutableStates.update { it + (date to SaveState.Saving(revision)) }
        val draft = Draft(DiaryEntry(date, text, clock.instant()), revision)
        pendingDrafts[date] = draft
        check(drafts.trySend(draft).isSuccess) { "Diary writer is unavailable" }
    }

    fun pendingText(date: LocalDate): String? = pendingDrafts[date]?.entry?.text

    private suspend fun persist(draft: Draft) {
        val result = try {
            repository.saveEntry(draft.entry)
            pendingDrafts.remove(draft.entry.date, draft)
            SaveState.Saved(draft.revision)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            SaveState.Failed(draft.revision)
        }
        mutableStates.update { states ->
            if (states[draft.entry.date] == SaveState.Saving(draft.revision)) {
                states + (draft.entry.date to result)
            } else {
                states
            }
        }
    }
}
