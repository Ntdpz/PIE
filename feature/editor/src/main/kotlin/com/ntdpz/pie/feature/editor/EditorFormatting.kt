package com.ntdpz.pie.feature.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import com.ntdpz.pie.core.domain.diary.TextFormat
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import com.ntdpz.pie.core.domain.diary.TextFormatting

internal data class EditorDocument(val value: TextFieldValue, val formatting: List<TextFormatRange>)

internal fun styledEditorText(text: String, formatting: List<TextFormatRange>): AnnotatedString =
    AnnotatedString.Builder(text).apply {
        for (range in TextFormatting.normalize(text, formatting)) {
            val style = when (range.format) {
                TextFormat.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
                TextFormat.Heading -> SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold)
                TextFormat.Subheading -> SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            addStyle(style, range.start, range.end)
        }
    }.toAnnotatedString()

internal fun paragraphSelection(value: TextFieldValue): TextRange {
    val start = value.text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(-1)) + 1
    val lastCharacter = if (value.selection.collapsed) value.selection.max else value.selection.max - 1
    val end = value.text.indexOf('\n', lastCharacter).takeIf { it >= 0 } ?: value.text.length
    return TextRange(start, end)
}

internal fun formatDocument(document: EditorDocument, format: TextFormat?): EditorDocument {
    val value = document.value
    val range = if (format == TextFormat.Bold && !value.selection.collapsed) value.selection else paragraphSelection(value)
    val start = range.min
    val end = range.max
    if (start == end) return document
    val remove = if (format == TextFormat.Bold) setOf(TextFormat.Bold)
        else setOf(TextFormat.Heading, TextFormat.Subheading)
    val covered = format != null && TextFormatting.covers(document.formatting, start, end, format)
    val cleared = TextFormatting.remove(document.formatting, start, end, remove)
    val updated = if (format == null || covered) cleared else cleared + TextFormatRange(start, end, format)
    return document.copy(formatting = TextFormatting.normalize(value.text, updated))
}

internal fun toggleBullets(document: EditorDocument): EditorDocument {
    val value = document.value
    val selection = paragraphSelection(value)
    val lines = value.text.substring(selection.min, selection.max).split('\n')
    val removing = lines.all { it.startsWith("• ") }
    var text = value.text
    var ranges = document.formatting
    var offset = selection.min
    for (line in lines) {
        val next = if (removing) text.removeRange(offset, offset + 2) else text.replaceRange(offset, offset, "• ")
        ranges = TextFormatting.afterEdit(text, next, ranges)
        text = next
        offset += line.length + (if (removing) -2 else 2) + 1
    }
    val lengthChange = text.length - value.text.length
    val updatedSelection = if (value.selection.collapsed) TextRange((value.selection.start + (if (removing) -2 else 2)).coerceIn(0, text.length))
        else TextRange(selection.min, selection.max + lengthChange)
    return EditorDocument(TextFieldValue(text, updatedSelection), ranges)
}

internal fun replaceSelection(document: EditorDocument, insertion: String): EditorDocument {
    val value = document.value
    val start = value.selection.min
    val text = value.text.replaceRange(start, value.selection.max, insertion)
    return EditorDocument(
        TextFieldValue(text, TextRange(start + insertion.length)),
        TextFormatting.afterEdit(value.text, text, document.formatting),
    )
}
