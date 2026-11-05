package com.ntdpz.pie.feature.history

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ntdpz.pie.core.domain.diary.TextFormat
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import com.ntdpz.pie.core.domain.diary.TextFormatting

internal fun styledMemoryText(text: String, formatting: List<TextFormatRange>): AnnotatedString =
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
