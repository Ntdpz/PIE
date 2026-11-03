package com.ntdpz.pie.feature.settings.reminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.ntdpz.pie.core.designsystem.component.PieDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import com.ntdpz.pie.feature.settings.SettingsViewModel
import java.time.LocalDate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderRoute(viewModel: SettingsViewModel, today: LocalDate, onBack: () -> Unit, notify: (ReminderSettings) -> Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var requestTest by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<Int?>(null) }
    var timePickerOpen by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { timePickerOpen = false }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) viewModel.showMessage("ยังไม่ได้เปิดสิทธิ์แจ้งเตือน")
        else if (requestTest) {
            viewModel.showMessage(if (notify(state.settings.reminder)) "ส่งแจ้งเตือนทดสอบแล้ว" else "โปรดเปิดการแจ้งเตือนของ PIE ในการตั้งค่าเครื่อง")
        } else viewModel.saveReminder(state.settings.reminder.copy(enabled = true))
    }
    fun hasPermission() = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    fun requestOrRun(test: Boolean) {
        requestTest = test
        if (!hasPermission()) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        else if (test) viewModel.showMessage(if (notify(state.settings.reminder)) "ส่งแจ้งเตือนทดสอบแล้ว" else "โปรดเปิดการแจ้งเตือนของ PIE ในการตั้งค่าเครื่อง")
        else viewModel.saveReminder(state.settings.reminder.copy(enabled = true))
    }
    ReminderScreen(
        state, today, onBack,
        onUpdate = viewModel::saveReminder,
        onEnable = { if (it) requestOrRun(false) else viewModel.saveReminder(state.settings.reminder.copy(enabled = false)) },
        onTest = { requestOrRun(true) },
        onEditTime = { previous ->
            editingTime = previous
            timePickerOpen = true
        },
    )
    if (timePickerOpen) {
        val initial = editingTime ?: 1260
        val picker = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
        PieDialog(
            onDismissRequest = { timePickerOpen = false },
            title = if (editingTime == null) "เพิ่มเวลา" else "เวลาแจ้งเตือน",
            text = { TimeInput(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    val times = state.settings.reminder.times.toMutableSet()
                    editingTime?.let(times::remove)
                    times.add(picker.hour * 60 + picker.minute)
                    viewModel.saveReminder(state.settings.reminder.copy(times = times))
                    timePickerOpen = false
                }) { Text("บันทึกเวลา") }
            },
            dismissButton = { TextButton(onClick = { timePickerOpen = false }) { Text("ยกเลิก") } },
        )
    }
}
