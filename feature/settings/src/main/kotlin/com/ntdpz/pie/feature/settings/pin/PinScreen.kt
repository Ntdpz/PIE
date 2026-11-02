package com.ntdpz.pie.feature.settings.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.component.*

@Composable
fun PinRoute(viewModel: PinViewModel, onComplete: () -> Unit, onBack: (() -> Unit)? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.completed) { if (state.completed) onComplete() }
    PinScreen(state, viewModel::digit, viewModel::backspace, onBack)
}

@Composable
fun PinScreen(state: PinUiState, onDigit: (Int) -> Unit, onDelete: () -> Unit, onBack: (() -> Unit)?) {
    var forgot by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            if (onBack != null) IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับ") }
        }
        Spacer(Modifier.height(12.dp))
        PieBrand()
        Spacer(Modifier.height(28.dp))
        val title = when (state.stage) {
            PinStage.New -> "ตั้ง PIN ของคุณ"
            PinStage.Confirm -> "ยืนยัน PIN อีกครั้ง"
            PinStage.Verify -> if (state.purpose == PinPurpose.Unlock) "กลับมาแล้ว :)" else "ใส่ PIN เดิม"
        }
        Text(title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.stage == PinStage.Verify && state.purpose == PinPurpose.Unlock) "ใส่ PIN 6 หลัก เพื่อเปิดบันทึกของคุณ" else "ใช้ตัวเลข 6 หลัก",
            style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(6) { index ->
                val filled = index < state.digits
                val color by animateColorAsState(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "PIN dot color")
                val scale = animateFloatAsState(if (filled) 1.2f else 1f, spring(dampingRatio = 0.55f, stiffness = 850f), label = "PIN dot pulse")
                Box(Modifier.size(12.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.background(color, CircleShape))
            }
        }
        Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
            if (state.busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else state.error?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2)).forEach { keys ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    keys.forEach { key ->
                        if (key == -1) Spacer(Modifier.weight(1f).height(68.dp))
                        else Surface(
                            onClick = { if (key == -2) onDelete() else onDigit(key) },
                            enabled = !state.busy, modifier = Modifier.weight(1f).height(68.dp),
                            shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == -2) PieIcon(PieGlyph.Delete, "ลบตัวเลข", color = MaterialTheme.colorScheme.onSurfaceVariant, size = 24.dp)
                                else Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        if (state.purpose == PinPurpose.Unlock) TextButton(onClick = { forgot = true }) { Text("ลืม PIN?") }
        else Text("PIE ไม่มีระบบกู้ PIN\nโปรดจดจำรหัสของคุณไว้", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (forgot) AlertDialog(
        onDismissRequest = { forgot = false }, title = { Text("ลืม PIN") },
        text = { Text("PIE ไม่มีระบบกู้ PIN หากต้องเริ่มใหม่ ต้องล้างข้อมูลแอปจากการตั้งค่า Android ซึ่งจะลบบันทึกทั้งหมดบนเครื่อง\n\nหากมีไฟล์สำรอง สามารถนำเข้าหลังเริ่มใหม่ด้วยรหัสผ่านไฟล์สำรองได้") },
        confirmButton = { TextButton(onClick = { forgot = false }) { Text("เข้าใจแล้ว") } },
    )
}
