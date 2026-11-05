package com.ntdpz.pie.core.domain.backup

import com.ntdpz.pie.core.domain.diary.DiaryEntry
import java.time.Instant
import java.time.LocalDate

enum class MergeChoice { KeepDevice, UseBackup, KeepBoth }

data class DiaryConflict(val date: LocalDate, val device: DiaryEntry, val backup: DiaryEntry)

data class DiaryMergePlan(
    val additions: List<DiaryEntry>,
    val unchangedDates: Set<LocalDate>,
    val conflicts: List<DiaryConflict>,
)

/** Pure text planning. File validation, media identity and persistence belong to other stations. */
object DiaryMergePlanner {
    fun preview(device: List<DiaryEntry>, backup: List<DiaryEntry>): DiaryMergePlan {
        require(device.map { it.date }.distinct().size == device.size)
        require(backup.map { it.date }.distinct().size == backup.size)
        val local = device.associateBy { it.date }
        val additions = mutableListOf<DiaryEntry>()
        val unchanged = mutableSetOf<LocalDate>()
        val conflicts = mutableListOf<DiaryConflict>()
        backup.sortedBy { it.date }.forEach { incoming ->
            val existing = local[incoming.date]
            when {
                existing == null -> additions += incoming
                existing.text == incoming.text && existing.formatting == incoming.formatting -> unchanged += incoming.date
                existing.text.isEmpty() -> additions += incoming
                incoming.text.isEmpty() -> unchanged += incoming.date
                else -> conflicts += DiaryConflict(incoming.date, existing, incoming)
            }
        }
        return DiaryMergePlan(additions, unchanged, conflicts)
    }

    fun resolve(conflict: DiaryConflict, choice: MergeChoice, now: Instant): DiaryEntry = when (choice) {
        MergeChoice.KeepDevice -> conflict.device
        MergeChoice.UseBackup -> conflict.backup.copy(updatedAt = now)
        MergeChoice.KeepBoth -> conflict.device.copy(
            text = conflict.device.text + "\n\n──────────\nข้อความจากไฟล์สำรอง\n──────────\n\n" + conflict.backup.text,
            updatedAt = now,
            formatting = conflict.device.formatting + conflict.backup.formatting.map {
                val offset = conflict.device.text.length + "\n\n──────────\nข้อความจากไฟล์สำรอง\n──────────\n\n".length
                it.copy(start = it.start + offset, end = it.end + offset)
            },
        )
    }
}
