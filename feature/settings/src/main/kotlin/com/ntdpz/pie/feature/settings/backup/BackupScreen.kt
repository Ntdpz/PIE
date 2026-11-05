package com.ntdpz.pie.feature.settings.backup

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*
import java.time.LocalDate

@Composable
fun BackupRoute(viewModel: BackupViewModel, restoring: Boolean, driveDestination: Boolean = false, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var source by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) viewModel.run(uri.toString(), password.toCharArray(), false)
        password = ""; confirmation = ""
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            source = uri
            sourceName = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { if (it.moveToFirst()) it.getString(0) else null }
            }.getOrNull() ?: "ไฟล์สำรองที่เลือก"
            password = ""
        }
    }
    val goBack: () -> Unit = {
        if (restoring && source != null) {
            focus.clearFocus()
            source = null
            sourceName = null
            password = ""
        } else onBack()
    }
    BackHandler(state.busy || (restoring && source != null && state.preview == null && state.result == null)) {
        if (!state.busy) goBack()
    }
    if (state.preview != null) {
        ImportReviewScreen(state, viewModel::choose, viewModel::confirmImport, viewModel::cancelPreview)
        return
    }
    if (restoring && state.result != null) {
        ImportSuccessScreen(requireNotNull(state.result), onBack)
        return
    }
    BackupScreen(
        state, restoring, driveDestination, sourceName, password, confirmation,
        { password = it }, { confirmation = it }, goBack,
        onPickFile = { open.launch(arrayOf("*/*")) },
        onContinue = {
            focus.clearFocus()
            if (restoring) {
                source?.let { viewModel.run(it.toString(), password.toCharArray(), true) }
                password = ""
            } else {
                create.launch("PIE-${LocalDate.now()}.pie")
            }
        },
    )
}

@Composable
private fun BackupScreen(
    state: BackupUiState,
    restoring: Boolean,
    driveDestination: Boolean,
    sourceName: String?,
    password: String,
    confirmation: String,
    onPassword: (String) -> Unit,
    onConfirmation: (String) -> Unit,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val picking = restoring && sourceName == null
    val valid = picking || (password.length >= 8 && (restoring || password == confirmation))
    Scaffold(
        modifier = Modifier.imePadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, enabled = !state.busy) {
                        PieIcon(PieGlyph.Previous, "กลับ", size = 20.dp)
                    }
                    Text(
                        if (restoring) "นำเข้าไฟล์สำรอง" else if (driveDestination) "สำรองไป Google Drive" else "ส่งออกไฟล์สำรอง",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 24.dp))
            }
        },
        bottomBar = {
            Surface {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimatedVisibility(state.busy, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        PieLoader(if (restoring) "กำลังตรวจไฟล์สำรอง…" else "กำลังเข้ารหัสและตรวจไฟล์สำรอง…", compact = true)
                    }
                    Text(
                        when {
                            restoring -> "คุณจะได้ตรวจรายการก่อนยืนยันการรวม"
                            driveDestination -> "ในหน้าถัดไป เลือก Drive แล้วเลือกโฟลเดอร์ที่ต้องการ"
                            else -> "กรอกและยืนยันรหัสผ่านเพื่อเลือกที่บันทึก"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                    PiePrimaryButton(
                        when {
                            picking -> "เลือกไฟล์ .pie"
                            restoring -> "เปิดไฟล์เพื่อตรวจรายการ"
                            driveDestination -> "เลือกที่เก็บบน Google Drive"
                            else -> "เลือกที่บันทึกไฟล์"
                        },
                        if (picking) onPickFile else onContinue,
                        enabled = valid && !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = picking,
            modifier = Modifier.fillMaxSize().padding(padding),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "backup step",
        ) { chooseFile ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (restoring) {
                    Text(
                        if (chooseFile) "1 จาก 2 · เลือกไฟล์" else "2 จาก 2 · เปิดไฟล์",
                        color = colors.primary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Text(
                    when {
                        chooseFile -> "เลือกไฟล์ .pie"
                        restoring -> "ใส่รหัสผ่านของไฟล์"
                        else -> "สร้างไฟล์สำรอง .pie"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (chooseFile) {
                    Text("เลือกไฟล์สำรองจากเครื่อง แล้วใส่รหัสผ่านที่ตั้งไว้ตอนส่งออก")
                } else {
                    if (restoring) {
                        Surface(color = colors.surface, shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("ไฟล์ที่เลือก", style = MaterialTheme.typography.labelMedium)
                                Text(sourceName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    } else {
                        Text("เก็บข้อความ รูป และวิดีโอไว้ในไฟล์เข้ารหัส")
                    }
                    PasswordField(password, onPassword, "รหัสผ่านไฟล์", !state.busy)
                    if (restoring) {
                        TextButton(onClick = onPickFile, enabled = !state.busy) { Text("เลือกไฟล์อื่น") }
                    } else {
                        Text("อย่างน้อย 8 ตัวอักษร · ใช้รหัสแยกจาก PIN เปิดแอป", style = MaterialTheme.typography.bodySmall)
                        PasswordField(confirmation, onConfirmation, "ยืนยันรหัสผ่าน", !state.busy)
                        Text("เก็บรหัสนี้ไว้สำหรับเปิดไฟล์เมื่อนำเข้าบันทึก", style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.message?.let {
                    val message = if (driveDestination && state.completed) {
                        "สร้างไฟล์สำรองแล้ว · ตรวจสถานะอัปโหลดในแอป Drive"
                    } else it
                    Text(message, style = MaterialTheme.typography.bodySmall, color = if (state.completed) colors.tertiary else colors.error)
                }
            }
        }
    }
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, placeholder: String, enabled: Boolean) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value, onChange, modifier = Modifier.fillMaxWidth(), enabled = enabled,
        singleLine = true, label = { Text(placeholder) },
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
        trailingIcon = { IconButton(onClick = { visible = !visible }) { PieIcon(if (visible) PieGlyph.EyeOff else PieGlyph.Eye, if (visible) "ซ่อนรหัสผ่าน" else "แสดงรหัสผ่าน", size = 20.dp) } },
    )
}
