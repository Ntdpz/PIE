package com.ntdpz.pie.core.data.backup

import com.ntdpz.pie.core.data.diary.PieDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Shared by diary, media and archive mutations. Recovery runs before the first access. */
internal class DatabaseMutationGate(private val database: PieDatabase, private val filesDir: File) {
    private val mutex = Mutex()
    private var recovered = false

    suspend fun <T> exclusive(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!recovered) {
                recover()
                recovered = true
            }
            block()
        }
    }

    private suspend fun recover() {
        val root = File(filesDir, "diary-media")
        val referenced = database.mediaDao().readAll().map { it.fileName }.toSet()
        File(filesDir, "merge-sessions").listFiles()?.filter { it.isDirectory }?.forEach { session ->
            File(session, "promoted.txt").takeIf { it.exists() }?.readLines()?.forEach { name ->
                if (name.matches(Regex("[0-9a-f-]{36}\\.[a-zA-Z0-9]+")) && name !in referenced) {
                    val file = File(root, name)
                    check(!file.exists() || file.delete()) { "Cannot clean interrupted media" }
                }
            }
            check(session.deleteRecursively()) { "Cannot clean interrupted import" }
        }
    }
}
