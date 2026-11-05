package com.ntdpz.pie.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.ntdpz.pie.core.domain.diary.*
import org.junit.Assert.*
import org.junit.Test

class EditorFormattingTest {
    @Test fun boldToggleSplitsSelectionAndNeverChangesText() {
        val doc = EditorDocument(TextFieldValue("hello world", TextRange(0, 5)), emptyList())
        val styled = formatDocument(doc, TextFormat.Bold)
        assertEquals("hello world", styled.value.text)
        assertEquals(listOf(TextFormatRange(0, 5, TextFormat.Bold)), styled.formatting)
        assertTrue(formatDocument(styled, TextFormat.Bold).formatting.isEmpty())
    }

    @Test fun bulletsToggleSelectedParagraphsAndPreserveBold() {
        val doc = EditorDocument(TextFieldValue("หนึ่ง\nสอง", TextRange(0, 9)), listOf(TextFormatRange(0, 5, TextFormat.Bold)))
        val updated = toggleBullets(doc)
        assertEquals("• หนึ่ง\n• สอง", updated.value.text)
        val restored = toggleBullets(updated)
        assertEquals(doc.value.text, restored.value.text)
        assertEquals(doc.formatting, restored.formatting)
    }

    @Test fun voiceReplacesSelectionAtExactCursorWithoutLosingStyles() {
        val doc = EditorDocument(TextFieldValue("Aวันนี้B", TextRange(1, 7)), listOf(TextFormatRange(0, 1, TextFormat.Bold)))
        val result = replaceSelection(doc, "ท้องฟ้า")
        assertEquals("Aท้องฟ้าB", result.value.text)
        assertEquals(TextRange(8), result.value.selection)
        assertEquals(doc.formatting, result.formatting)
    }
}
