package com.ntdpz.pie.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.backup.*
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val completed: Boolean = false,
    val preview: ImportPreview? = null,
    val choices: Map<LocalDate, MergeChoice> = emptyMap(),
    val result: ImportResult? = null,
)

class BackupViewModel(
    private val repository: BackupRepository,
    private val writer: DiaryWriter,
    private val imports: MergeImportRepository,
    private val cleanupScope: CoroutineScope,
) : ViewModel() {
    private val mutable = MutableStateFlow(BackupUiState())
    val state = mutable.asStateFlow()
    private var preparedToken: String? = null

    fun run(uri: String, password: CharArray, restore: Boolean) {
        if (state.value.busy) { password.fill('\u0000'); return }
        mutable.value = BackupUiState(busy = true)
        viewModelScope.launch {
            try {
                preparedToken?.let { imports.discardImport(it) }
                preparedToken = null
                writer.awaitIdle()
                if (restore) {
                    val preview = imports.prepareImport(uri, password)
                    preparedToken = preview.token
                    mutable.value = BackupUiState(preview = preview, choices = preview.conflicts.associate { it.date to MergeChoice.KeepDevice })
                } else {
                    repository.export(uri, password)
                    mutable.value = BackupUiState(completed = true, message = "ส่งออกไฟล์สำรองแล้ว")
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: BackupReadException) {
                mutable.value = BackupUiState(message = error.reason.userMessage())
            }
            catch (_: Exception) {
                mutable.value = BackupUiState(message = if (restore) "ตรวจไฟล์ไม่สำเร็จ โปรดตรวจไฟล์ รหัสผ่าน และพื้นที่ว่าง ข้อมูลเดิมยังอยู่" else "ส่งออกไม่สำเร็จ โปรดตรวจพื้นที่ว่างและสถานะการบันทึก")
            } finally { password.fill('\u0000') }
        }
    }

    fun choose(date: LocalDate, choice: MergeChoice) {
        if (state.value.busy || date !in state.value.choices) return
        mutable.update { it.copy(choices = it.choices + (date to choice)) }
    }

    fun confirmImport() {
        val snapshot = state.value
        val preview = snapshot.preview ?: return
        if (snapshot.busy) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                writer.awaitIdle()
                val result = imports.commitImport(preview.token, snapshot.choices)
                mutable.value = BackupUiState(completed = true, result = result, message = "นำเข้าบันทึกแล้ว · PIN ธีม และแจ้งเตือนยังใช้ค่าของเครื่องนี้")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: ImportPreviewChanged) {
                mutable.value = BackupUiState(message = "บันทึกในเครื่องเปลี่ยนหลังตรวจไฟล์ กรุณาเลือกไฟล์เพื่อตรวจใหม่ ข้อมูลเดิมยังอยู่")
            } catch (_: Exception) {
                mutable.value = BackupUiState(message = "นำเข้าไม่สำเร็จ โปรดตรวจพื้นที่ว่างแล้วเลือกไฟล์ใหม่ ข้อมูลเดิมยังอยู่")
            } finally { preparedToken = null }
        }
    }

    fun cancelPreview() {
        if (state.value.busy) return
        val token = preparedToken ?: return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { imports.discardImport(token) }
            finally { preparedToken = null; mutable.value = BackupUiState() }
        }
    }

    override fun onCleared() {
        preparedToken?.let { token -> cleanupScope.launch { imports.discardImport(token) } }
    }
}

private fun BackupReadFailure.userMessage(): String = when (this) {
    BackupReadFailure.Incomplete -> "ไฟล์สำรองบันทึกไม่ครบ กรุณาส่งออกไฟล์ใหม่จากเครื่องที่มีบันทึกเดิม ข้อมูลในแอปยังอยู่"
    BackupReadFailure.AuthenticationOrDamage -> "รหัสผ่านไม่ตรงหรือไฟล์สำรองเสียหาย โปรดลองรหัสผ่านของไฟล์นี้ หรือส่งออกไฟล์ใหม่ ข้อมูลเดิมยังอยู่"
    BackupReadFailure.Unsupported -> "ไฟล์นี้ไม่ใช่ไฟล์สำรอง PIE ที่รองรับ กรุณาเลือกไฟล์ .pie ที่ส่งออกจาก PIE"
    BackupReadFailure.LegacyTooLarge -> "ไฟล์สำรองรุ่นเก่ามีขนาดใหญ่ กรุณาอัปเดต PIE บนเครื่องเดิมแล้วส่งออกไฟล์ใหม่"
}
