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
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack, enabled = !state.busy) { PieIcon(PieGlyph.Back, "กลับ") }
            Text("ตั้งค่า", style = MaterialTheme.typography.titleSmall, color = colors.onSurfaceVariant)
        }
        Text(if (restoring) "กลับมาหาเรื่องเล่าเดิม" else "เก็บบันทึกไว้อีกที่", style = MaterialTheme.typography.headlineLarge, fontSize = 28.sp)
        Text(
            if (restoring) "นำเข้าข้อความ รูป และวิดีโอ\nจากไฟล์สำรองของคุณ" else "ส่งออกข้อความ รูป และวิดีโอทั้งหมด\nเป็นไฟล์สำรองหนึ่งไฟล์",
            color = colors.onSurfaceVariant,
        )
        Surface(color = colors.primaryContainer, contentColor = colors.onBackground, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PieIcon(PieGlyph.Archive, color = colors.primary, size = 28.dp)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("บันทึกทั้งหมดของคุณ")
                    Text(if (restoring) "ตรวจรายการก่อนรวมกับบันทึกในเครื่อง" else "ใช้ย้ายข้อมูลเข้า PIE บนเครื่องใหม่ได้", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (restoring) "รหัสผ่านของไฟล์สำรอง" else "ตั้งรหัสผ่านให้ไฟล์สำรอง", style = MaterialTheme.typography.bodySmall)
            PasswordField(password, onPassword, "รหัสผ่านไฟล์สำรอง", !state.busy)
            if (!restoring) PasswordField(confirmation, onConfirmation, "ยืนยันรหัสผ่านอีกครั้ง", !state.busy)
            Text("ตั้งแยกจาก PIN ที่ใช้เปิดแอป\nคุณต้องใช้รหัสผ่านนี้เมื่อนำเข้าไฟล์", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
        Surface(color = colors.surface, shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PieIcon(PieGlyph.Info, color = colors.primary, size = 20.dp)
                Text("PIE กู้รหัสผ่านไฟล์สำรองให้ไม่ได้\nโปรดเก็บรหัสผ่านไว้ในที่ที่คุณจำได้", style = MaterialTheme.typography.labelMedium)
            }
        }
        PiePrimaryButton(
            if (state.busy) "กำลังทำรายการ…" else if (restoring) "เลือกไฟล์เพื่อตรวจรายการ" else "เลือกที่เก็บและส่งออก",
            onChoose, enabled = valid && !state.busy, icon = PieGlyph.Download,
        )
        AnimatedVisibility(state.busy, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            PieLoader(if (restoring) "กำลังตรวจไฟล์สำรอง…" else "กำลังเข้ารหัสและตรวจไฟล์สำรอง…", compact = true)
        }
        state.result?.let { result ->
            Text("เพิ่ม ${result.addedDays} วัน · แก้ไข ${result.changedDays} วัน · ข้าม ${result.skippedDays} วัน\nเพิ่มสื่อ ${result.addedMedia} รายการ · ข้ามสื่อซ้ำ ${result.skippedMedia} รายการ")
        }
        Text(
            state.message ?: if (restoring) "กรอกรหัสผ่านของไฟล์ที่ต้องการนำเข้า" else "กรอกรหัสผ่านอย่างน้อย 8 ตัวให้ตรงกันเพื่อส่งออก",
            style = MaterialTheme.typography.labelSmall,
            color = if (state.message != null && !state.completed) colors.error else colors.onSurfaceVariant,
        )
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
