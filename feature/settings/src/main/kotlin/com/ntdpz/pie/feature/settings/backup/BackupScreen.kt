package com.ntdpz.pie.feature.settings.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import java.time.LocalDate

@Composable
fun BackupRoute(viewModel: BackupViewModel, restoring: Boolean, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.run(uri.toString(), password.toCharArray(), false)
        password = ""; confirmation = ""
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.run(uri.toString(), password.toCharArray(), true)
        password = ""; confirmation = ""
    }
    BackHandler(state.busy) { }
    if (state.preview != null) {
        ImportReviewScreen(state, viewModel::choose, viewModel::confirmImport, viewModel::cancelPreview)
        return
    }
    if (restoring && state.result != null) {
        ImportSuccessScreen(requireNotNull(state.result), onBack)
        return
    }
    BackupScreen(
        state, restoring, password, confirmation,
        { password = it }, { confirmation = it }, onBack,
        onChoose = {
            if (restoring) open.launch(arrayOf("*/*"))
            else create.launch("PIE-${LocalDate.now()}.pie")
        },
    )
}

@Composable
fun BackupScreen(
    state: BackupUiState,
    restoring: Boolean,
    password: String,
    confirmation: String,
    onPassword: (String) -> Unit,
    onConfirmation: (String) -> Unit,
    onBack: () -> Unit,
    onChoose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val valid = password.length >= 8 && (restoring || password == confirmation)
    Scaffold(
        modifier = Modifier.imePadding(), contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AnimatedVisibility(state.busy, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        PieLoader(if (restoring) "กำลังตรวจไฟล์สำรอง…" else "กำลังเข้ารหัสและตรวจไฟล์สำรอง…", compact = true)
                    }
                    PiePrimaryButton(if (restoring) "เลือกไฟล์ .pie" else "ส่งออกไฟล์", onChoose, enabled = valid && !state.busy, icon = if (restoring) PieGlyph.Archive else PieGlyph.Download)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            TextButton(onClick = onBack, enabled = !state.busy) { PieIcon(PieGlyph.Previous, size = 18.dp); Text("สำรองข้อมูล") }
            Text(if (restoring) "พาเรื่องเล่ากลับมา" else "เก็บไว้อีกที่", style = MaterialTheme.typography.headlineLarge)
            Surface(color = colors.surface, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PieIcon(PieGlyph.Archive, color = colors.primary, size = 40.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (restoring) ".pie" else "ทุกเรื่องในไฟล์เดียว", style = MaterialTheme.typography.titleMedium)
                        Text(if (restoring) "รวมกับบันทึกเดิม" else "ข้อความ · รูป · วิดีโอ", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (restoring) "รหัสผ่านของไฟล์" else "ตั้งรหัสผ่านของไฟล์", style = MaterialTheme.typography.bodySmall)
                PasswordField(password, onPassword, "รหัสผ่านไฟล์สำรอง", !state.busy)
                if (!restoring) {
                    Text("ยืนยันรหัสผ่าน", style = MaterialTheme.typography.bodySmall)
                    PasswordField(confirmation, onConfirmation, "ยืนยันรหัสผ่านอีกครั้ง", !state.busy)
                }
                Text(if (restoring) "รหัสที่ตั้งไว้ตอนส่งออก" else "อย่างน้อย 8 ตัวอักษร · ใช้รหัสแยกจาก PIN", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (state.completed) colors.tertiary else colors.error) }
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, placeholder: String, enabled: Boolean) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        singleLine = true, placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        trailingIcon = { IconButton(onClick = { visible = !visible }) { PieIcon(if (visible) PieGlyph.EyeOff else PieGlyph.Eye, if (visible) "ซ่อนรหัสผ่าน" else "แสดงรหัสผ่าน", size = 20.dp) } },
    )
}
