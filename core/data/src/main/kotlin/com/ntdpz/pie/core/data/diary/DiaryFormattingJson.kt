package com.ntdpz.pie.core.data.diary

import com.ntdpz.pie.core.domain.diary.TextFormat
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import com.ntdpz.pie.core.domain.diary.TextFormatting
import org.json.JSONArray
import org.json.JSONObject

internal object DiaryFormattingJson {
    fun encode(text: String, ranges: List<TextFormatRange>): String = JSONArray().apply {
        TextFormatting.normalize(text, ranges).forEach { range ->
            put(JSONObject().put("start", range.start).put("end", range.end).put("format", range.format.name))
        }
    }.toString()

    fun decode(text: String, json: String): List<TextFormatRange> {
        val array = JSONArray(json)
        val ranges = (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val start = item.getInt("start")
            val end = item.getInt("end")
            require(start >= 0 && end > start && end <= text.length)
            TextFormatRange(start, end, TextFormat.valueOf(item.getString("format")))
        }
        return TextFormatting.normalize(text, ranges)
    }
}
