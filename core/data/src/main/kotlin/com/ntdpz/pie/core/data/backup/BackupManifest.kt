package com.ntdpz.pie.core.data.backup

import android.util.JsonReader
import android.util.JsonWriter
import com.ntdpz.pie.core.data.diary.DiaryEntity
import com.ntdpz.pie.core.data.media.MediaEntity
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.UUID
import com.ntdpz.pie.core.data.media.MediaFileNames

internal data class BackupManifest(
    val entries: List<DiaryEntity>,
    val media: List<MediaEntity>,
    val reminder: ReminderSettings,
)

internal object BackupManifestIO {
    fun write(output: OutputStream, manifest: BackupManifest) {
        val writer = JsonWriter(output.writer(Charsets.UTF_8))
        writer.beginObject().name("version").value(1)
        writer.name("entries").beginArray()
        manifest.entries.forEach {
            writer.beginObject().name("date").value(it.date).name("text").value(it.text)
                .name("updatedAt").value(it.updatedAtEpochMillis).endObject()
        }
        writer.endArray().name("media").beginArray()
        manifest.media.forEach {
            writer.beginObject().name("id").value(it.id).name("date").value(it.date)
                .name("mime").value(it.mimeType).name("createdAt").value(it.createdAtEpochMillis).endObject()
        }
        writer.endArray().name("reminder").beginObject()
            .name("custom").value(manifest.reminder.customMode).name("message").value(manifest.reminder.message)
            .name("days").beginArray()
        manifest.reminder.days.sorted().forEach { writer.value(it) }
        writer.endArray().name("times").beginArray()
        manifest.reminder.times.sorted().forEach { writer.value(it) }
        writer.endArray().endObject().endObject()
        writer.flush()
    }

    fun read(input: InputStream): BackupManifest {
        val reader = JsonReader(input.reader(Charsets.UTF_8))
        var version = 0
        val entries = mutableListOf<DiaryEntity>()
        val media = mutableListOf<MediaEntity>()
        var reminder = ReminderSettings()
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "version" -> version = reader.nextInt()
            "entries" -> {
                reader.beginArray()
                while (reader.hasNext()) entries += readEntry(reader)
                reader.endArray()
            }
            "media" -> {
                reader.beginArray()
                while (reader.hasNext()) media += readMedia(reader)
                reader.endArray()
            }
            "reminder" -> reminder = readReminder(reader)
            else -> reader.skipValue()
        }
        reader.endObject()
        require(version == 1 && entries.map { it.date }.distinct().size == entries.size)
        require(media.map { it.id }.distinct().size == media.size)
        val dates = entries.map { it.date }.toSet()
        require(media.all { it.date in dates })
        return BackupManifest(entries, media, reminder)
    }

    private fun readEntry(reader: JsonReader): DiaryEntity {
        var date: String? = null; var text: String? = null; var updated = 0L
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "date" -> date = reader.nextString()
            "text" -> text = reader.nextString()
            "updatedAt" -> updated = reader.nextLong()
            else -> reader.skipValue()
        }
        reader.endObject()
        val key = requireNotNull(date).also { require(LocalDate.parse(it).toString() == it) }
        return DiaryEntity(key, requireNotNull(text), updated)
    }

    private fun readMedia(reader: JsonReader): MediaEntity {
        var id: String? = null; var date: String? = null; var mime: String? = null; var created = 0L
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "id" -> id = reader.nextString()
            "date" -> date = reader.nextString()
            "mime" -> mime = reader.nextString()
            "createdAt" -> created = reader.nextLong()
            else -> reader.skipValue()
        }
        reader.endObject()
        val key = requireNotNull(id).also { require(UUID.fromString(it).toString() == it) }
        val type = requireNotNull(mime).also { require(it.startsWith("image/") || it.startsWith("video/")) }
        return MediaEntity(key, requireNotNull(date), MediaFileNames.forId(key, type), type, created)
    }

    private fun readReminder(reader: JsonReader): ReminderSettings {
        var custom = false; var message = ""; var days = (1..7).toSet(); var times = setOf(1260)
        fun integers(): Set<Int> {
            val result = mutableSetOf<Int>(); reader.beginArray()
            while (reader.hasNext()) result += reader.nextInt()
            reader.endArray(); return result
        }
        reader.beginObject()
        while (reader.hasNext()) when (reader.nextName()) {
            "custom" -> custom = reader.nextBoolean()
            "message" -> message = reader.nextString()
            "days" -> days = integers()
            "times" -> times = integers()
            else -> reader.skipValue()
        }
        reader.endObject()
        require(days.all { it in 1..7 } && times.all { it in 0..1439 })
        return ReminderSettings(false, days, times, custom, message)
    }
}
