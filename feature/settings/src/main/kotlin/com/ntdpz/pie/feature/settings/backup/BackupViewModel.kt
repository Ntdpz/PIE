package com.ntdpz.pie.feature.settings.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntdpz.pie.core.domain.backup.BackupRepository
import com.ntdpz.pie.core.domain.diary.DiaryWriter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class BackupUiState(val busy: Boolean = false, val message: String? = null, val completed: Boolean = false)

class BackupViewModel(private val repository: BackupRepository, private val writer: DiaryWriter) : ViewModel() {
    private val mutable = MutableStateFlow(BackupUiState())
    val state = mutable.asStateFlow()

    fun run(uri: String, password: CharArray, restore: Boolean) {
        if (state.value.busy) { password.fill('\u0000'); return }
        mutable.value = BackupUiState(busy = true)
        viewModelScope.launch {
            try {
                writer.awaitIdle()
                if (restore) repository.restore(uri, password) else repository.export(uri, password)
                mutable.value = BackupUiState(completed = true, message = if (restore) "นำเข้าบันทึกครบแล้ว กรุณาตั้ง PIN และเปิดแจ้งเตือนใหม่ตามต้องการ" else "ส่งออกไฟล์สำรองแล้ว")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                mutable.value = BackupUiState(message = if (restore) "นำเข้าไม่สำเร็จ โปรดตรวจไฟล์ รหัสผ่าน และพื้นที่ว่าง แอปต้องยังไม่มีข้อมูล" else "ส่งออกไม่สำเร็จ โปรดตรวจพื้นที่ว่างและสถานะการบันทึก")
            } finally { password.fill('\u0000') }
        }
    }
}
