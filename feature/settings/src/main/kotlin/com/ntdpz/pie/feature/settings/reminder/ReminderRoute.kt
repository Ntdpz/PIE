package com.ntdpz.pie.feature.settings.reminder

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.domain.settings.ReminderSettings
import com.ntdpz.pie.feature.settings.SettingsViewModel
import java.time.LocalDate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun ReminderRoute(viewModel: SettingsViewModel, today: LocalDate, onBack: () -> Unit, notify: (ReminderSettings) -> Boolean) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var requestTest by remember { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf<TimePickerDialog?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { timePicker?.dismiss(); timePicker = null }
    DisposableEffect(Unit) { onDispose { timePicker?.dismiss() } }
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
            val initial = previous ?: 1260
            timePicker = TimePickerDialog(context, { _, hour, minute ->
                val times = state.settings.reminder.times.toMutableSet()
                if (previous != null) times.remove(previous)
                times.add(hour * 60 + minute)
                viewModel.saveReminder(state.settings.reminder.copy(times = times))
            }, initial / 60, initial % 60, true).also { it.show() }
        },
    )
}
