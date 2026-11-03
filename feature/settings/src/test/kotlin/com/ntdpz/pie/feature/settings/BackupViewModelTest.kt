package com.ntdpz.pie.feature.settings

import androidx.lifecycle.ViewModelStore
import com.ntdpz.pie.core.domain.backup.*
import com.ntdpz.pie.core.domain.diary.*
import com.ntdpz.pie.feature.settings.backup.BackupViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun main() { Dispatchers.setMain(dispatcher) }
    @After fun reset() { Dispatchers.resetMain() }
    private val date = LocalDate.of(2026, 11, 3)

    @Test fun prepareDoesNotImportAndConfirmationUsesSelectedChoices() = runTest(dispatcher) {
        val imports = FakeImports(date)
        val writer = DiaryWriter(EmptyDiary(), backgroundScope, Clock.systemUTC())
        val vm = BackupViewModel(NoArchives(), writer, imports, backgroundScope)
        val store = ViewModelStore().apply { put("backup", vm) }
        val password = "test-password".toCharArray()
        vm.run("fixture", password, true)
        runCurrent()
        assertNotNull(vm.state.value.preview)
        assertNull(imports.committed)
        assertEquals(MergeChoice.KeepDevice, vm.state.value.choices[date])
        assertTrue(password.all { it == '\u0000' })
        vm.choose(date, MergeChoice.KeepBoth)
        vm.confirmImport()
        vm.confirmImport()
        runCurrent()
        assertEquals(mapOf(date to MergeChoice.KeepBoth), imports.committed)
        assertEquals(1, imports.commits)
        assertTrue(vm.state.value.completed)
        assertEquals(1, vm.state.value.result!!.changedDays)
        store.clear()
    }

    @Test fun stalePreviewReturnsToFileSelectionWithoutReportingSuccess() = runTest(dispatcher) {
        val imports = FakeImports(date).apply { stale = true }
        val vm = BackupViewModel(NoArchives(), DiaryWriter(EmptyDiary(), backgroundScope, Clock.systemUTC()), imports, backgroundScope)
        val store = ViewModelStore().apply { put("backup", vm) }
        vm.run("fixture", "test-password".toCharArray(), true)
        runCurrent()
        vm.confirmImport()
        runCurrent()
        assertFalse(vm.state.value.completed)
        assertNull(vm.state.value.preview)
        assertTrue(vm.state.value.message!!.contains("ตรวจใหม่"))
        store.clear()
    }

    @Test fun leavingReviewDiscardsStagedSession() = runTest(dispatcher) {
        val imports = FakeImports(date)
        val vm = BackupViewModel(NoArchives(), DiaryWriter(EmptyDiary(), backgroundScope, Clock.systemUTC()), imports, backgroundScope)
        val store = ViewModelStore().apply { put("backup", vm) }
        vm.run("fixture", "test-password".toCharArray(), true)
        runCurrent()
        store.clear()
        runCurrent()
        assertEquals(listOf("preview-token"), imports.discarded)
        assertNull(imports.committed)
    }

    @Test fun incompleteBackupExplainsReExportWithoutChangingExistingData() = runTest(dispatcher) {
        val imports = FakeImports(date).apply { readFailure = BackupReadFailure.Incomplete }
        val vm = BackupViewModel(NoArchives(), DiaryWriter(EmptyDiary(), backgroundScope, Clock.systemUTC()), imports, backgroundScope)
        val store = ViewModelStore().apply { put("backup", vm) }
        val password = "fixture-password".toCharArray()
        vm.run("fixture", password, true)
        runCurrent()
        assertTrue(vm.state.value.message!!.contains("บันทึกไม่ครบ"))
        assertTrue(vm.state.value.message!!.contains("ส่งออกไฟล์ใหม่"))
        assertFalse(vm.state.value.completed)
        assertNull(vm.state.value.preview)
        assertNull(imports.committed)
        assertTrue(password.all { it == '\u0000' })
        store.clear()
    }
    @Test fun failedValidationClearsPasswordAndDoesNotExposePreview() = runTest(dispatcher) {
        val imports = FakeImports(date).apply { failPrepare = true }
        val vm = BackupViewModel(NoArchives(), DiaryWriter(EmptyDiary(), backgroundScope, Clock.systemUTC()), imports, backgroundScope)
        val store = ViewModelStore().apply { put("backup", vm) }
        val password = "wrong-password".toCharArray()
        vm.run("fixture", password, true)
        runCurrent()
        assertTrue(password.all { it == '\u0000' })
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.completed)
        assertNull(vm.state.value.preview)
        store.clear()
    }
}

private class FakeImports(date: LocalDate) : MergeImportRepository {
    override val lastImportAtMillis = MutableStateFlow<Long?>(null)
    private val preview = ImportPreview("preview-token", 0, 0, listOf(DiaryConflict(date, DiaryEntry(date, "device", Instant.EPOCH), DiaryEntry(date, "backup", Instant.EPOCH))), 1, 0)
    var committed: Map<LocalDate, MergeChoice>? = null
    var commits = 0
    var stale = false
    var failPrepare = false
    var readFailure: BackupReadFailure? = null
    val discarded = mutableListOf<String>()
    override suspend fun prepareImport(uri: String, password: CharArray): ImportPreview {
        readFailure?.let { throw BackupReadException(it) }
        if (failPrepare) error("Invalid fixture")
        return preview
    }
    override suspend fun commitImport(token: String, choices: Map<LocalDate, MergeChoice>): ImportResult {
        if (stale) throw ImportPreviewChanged()
        commits++
        committed = choices
        return ImportResult(0, 1, 0, 1, 0)
    }
    override suspend fun discardImport(token: String) { discarded += token }
}

private class NoArchives : BackupRepository {
    override suspend fun export(uri: String, password: CharArray) = Unit
    override suspend fun restore(uri: String, password: CharArray) = error("Legacy restore must not be used")
    override suspend fun canRestore() = false
}

private class EmptyDiary : DiaryRepository {
    override fun observeEntry(date: LocalDate): Flow<DiaryEntry?> = flowOf(null)
    override fun observeSummaries(): Flow<List<DiarySummary>> = flowOf(emptyList())
    override suspend fun readEntry(date: LocalDate): DiaryEntry? = null
    override suspend fun saveEntry(entry: DiaryEntry) = Unit
    override suspend fun deleteEntry(date: LocalDate) = Unit
}
