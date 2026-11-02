package com.ntdpz.pie.core.domain.settings

import java.time.LocalDate

object ReminderMessages {
    private val prompts = listOf(
        "วันนี้มีอะไรอยากเล่าให้ฟังไหม?", "เรื่องเล็ก ๆ ของวันนี้ เราอยากฟังนะ",
        "เหนื่อยไหม แวะมาวางความรู้สึกไว้ตรงนี้ได้", "วันนี้เป็นยังไง มาเล่าได้ทุกเรื่องเลย",
        "มีอะไรอยู่ในใจ ลองเขียนออกมาสักนิดไหม?", "ไม่ต้องเป็นวันที่ดี ก็มีเรื่องให้เล่าได้นะ",
        "แวะมาหาตัวเองสักครู่ไหม?", "วันนี้มีอะไรที่อยากเก็บไว้จำบ้าง?",
        "จะบ่นหรือจะเล่า เราอยู่ตรงนี้นะ", "วันนี้ใจของคุณเป็นยังไงบ้าง?",
        "มีช่วงเวลาไหนที่อยากเล่าให้ตัวเองฟังไหม?", "พักจากทุกอย่าง แล้วมาฟังใจตัวเองกัน",
    )
    fun forDay(date: LocalDate, reminder: ReminderSettings): String =
        if (reminder.customMode && reminder.message.isNotBlank()) reminder.message
        else prompts[Math.floorMod(date.toEpochDay(), prompts.size.toLong()).toInt()]
}
