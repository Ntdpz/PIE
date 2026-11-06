package com.ntdpz.pie.core.domain.pet

import com.ntdpz.pie.core.domain.diary.SaveState
import java.time.LocalDate

/** One celebration after leaving an edited entry, only when that entry is persisted. */
class PetSaveCelebration {
    private data class Visit(val date: LocalDate, val baseline: Long, var mediaCommitted: Boolean = false)
    private var editing: Visit? = null
    private var pending: Visit? = null

    fun begin(date: LocalDate, states: Map<LocalDate, SaveState>) {
        pending = null
        editing = Visit(date, revision(states[date]))
    }

    fun finish(date: LocalDate, states: Map<LocalDate, SaveState>): Boolean {
        val visit = editing?.takeIf { it.date == date } ?: return false
        editing = null
        pending = visit
        return observe(states)
    }

    fun mediaCommitted(date: LocalDate) {
        editing?.takeIf { it.date == date }?.mediaCommitted = true
    }

    fun observe(states: Map<LocalDate, SaveState>): Boolean {
        val visit = pending ?: return false
        val state = states[visit.date]
        if (state is SaveState.Saving || state is SaveState.Failed) return false
        val textCommitted = state is SaveState.Saved && state.revision > visit.baseline
        if (!textCommitted && !visit.mediaCommitted) return false
        pending = null
        return true
    }

    fun dismiss() {
        pending = null
    }

    private fun revision(state: SaveState?): Long = when (state) {
        is SaveState.Saving -> state.revision
        is SaveState.Saved -> state.revision
        is SaveState.Failed -> state.revision
        else -> 0L
    }
}
