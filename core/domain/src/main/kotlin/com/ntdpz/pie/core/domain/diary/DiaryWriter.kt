package com.ntdpz.pie.core.domain.diary

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** One app-scoped station serializes writes, deletion and backup barriers. */
class DiaryWriter(
    private val repository: DiaryRepository,
    scope: CoroutineScope,
    private val clock: Clock,
) {
    private sealed interface Command {
        data class Draft(val entry: DiaryEntry, val revision: Long) : Command
        data class Delete(val date: LocalDate, val result: CompletableDeferred<Result<Unit>>) : Command
        data class Barrier(val result: CompletableDeferred<Unit>) : Command
    }

    private val revisions = AtomicLong()
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val pendingDrafts = ConcurrentHashMap<LocalDate, Command.Draft>()
    private val mutableStates = MutableStateFlow<Map<LocalDate, SaveState>>(emptyMap())
    val states = mutableStates.asStateFlow()

    init {
        scope.launch {
            for (command in commands) when (command) {
                is Command.Draft -> persist(command)
                is Command.Delete -> delete(command)
                is Command.Barrier -> command.result.complete(Unit)
            }
        }
    }

    fun submit(date: LocalDate, text: String, formatting: List<TextFormatRange> = emptyList()) {
        val revision = revisions.incrementAndGet()
        mutableStates.update { it + (date to SaveState.Saving(revision)) }
        val draft = Command.Draft(DiaryEntry(date, text, clock.instant(), TextFormatting.normalize(text, formatting)), revision)
        pendingDrafts[date] = draft
        check(commands.trySend(draft).isSuccess) { "Diary writer is unavailable" }
    }

    fun pendingEntry(date: LocalDate): DiaryEntry? = pendingDrafts[date]?.entry

    fun pendingText(date: LocalDate): String? = pendingDrafts[date]?.entry?.text

    suspend fun delete(date: LocalDate): Result<Unit> {
        val result = CompletableDeferred<Result<Unit>>()
        commands.send(Command.Delete(date, result))
        return result.await()
    }

    suspend fun awaitIdle() {
        val result = CompletableDeferred<Unit>()
        commands.send(Command.Barrier(result))
        result.await()
        check(states.value.values.none { it is SaveState.Failed }) { "Some drafts are not saved" }
    }

    private suspend fun persist(draft: Command.Draft) {
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
            } else states
        }
    }

    private suspend fun delete(command: Command.Delete) {
        try {
            repository.deleteEntry(command.date)
            pendingDrafts.remove(command.date)
            mutableStates.update { it - command.date }
            command.result.complete(Result.success(Unit))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            command.result.complete(Result.failure(error))
        }
    }
}
