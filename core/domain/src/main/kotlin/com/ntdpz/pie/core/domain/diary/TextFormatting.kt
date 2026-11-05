package com.ntdpz.pie.core.domain.diary

enum class TextFormat { Bold, Heading, Subheading }

data class TextFormatRange(val start: Int, val end: Int, val format: TextFormat) {
    init { require(start >= 0 && end > start) }
}

/** UTF-16 offsets match Android text selection, including Thai composition and emoji. */
object TextFormatting {
    fun normalize(text: String, ranges: List<TextFormatRange>): List<TextFormatRange> {
        val result = mutableListOf<TextFormatRange>()
        for (format in TextFormat.entries) {
            for (range in ranges.filter { it.format == format && it.start < text.length }
                .map { it.copy(end = it.end.coerceAtMost(text.length)) }.sortedBy { it.start }) {
                val previous = result.lastOrNull()
                if (previous?.format == format && previous.end >= range.start) {
                    result[result.lastIndex] = previous.copy(end = maxOf(previous.end, range.end))
                } else result += range
            }
        }
        return result
    }

    fun afterEdit(oldText: String, newText: String, ranges: List<TextFormatRange>, extendAtEnd: Boolean = false): List<TextFormatRange> {
        if (oldText == newText) return normalize(newText, ranges)
        val prefix = oldText.commonPrefixWith(newText).length
        val suffix = oldText.drop(prefix).commonSuffixWith(newText.drop(prefix)).length
        val oldEnd = oldText.length - suffix
        val newEnd = newText.length - suffix
        val delta = newEnd - oldEnd
        val adjusted = ranges.mapNotNull { range ->
            val start = when {
                range.start <= prefix -> range.start
                range.start >= oldEnd -> range.start + delta
                else -> prefix
            }
            val end = when {
                range.end <= prefix && !(extendAtEnd && oldEnd == prefix && range.end == prefix) -> range.end
                range.end >= oldEnd -> range.end + delta
                else -> newEnd
            }
            if (end > start) TextFormatRange(start, end, range.format) else null
        }
        return normalize(newText, adjusted)
    }

    fun remove(ranges: List<TextFormatRange>, start: Int, end: Int, formats: Set<TextFormat>): List<TextFormatRange> =
        ranges.flatMap { range ->
            if (range.format !in formats || range.end <= start || range.start >= end) listOf(range)
            else buildList {
                if (range.start < start) add(range.copy(end = start))
                if (range.end > end) add(range.copy(start = end))
            }
        }

    fun covers(ranges: List<TextFormatRange>, start: Int, end: Int, format: TextFormat): Boolean =
        end > start && ranges.any { it.format == format && it.start <= start && it.end >= end }
}
