package com.ntdpz.pie.core.domain.backup

interface BackupRepository {
    suspend fun export(uri: String, password: CharArray)
    suspend fun restore(uri: String, password: CharArray)
    suspend fun canRestore(): Boolean
}
