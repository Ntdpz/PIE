package com.ntdpz.pie.feature.settings.drive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.PieGlyph
import com.ntdpz.pie.core.designsystem.component.PieIcon

/** The Android document provider owns Drive access; PIE never claims an OAuth connection. */
@Composable
fun DriveScreen(onBack: () -> Unit, onExport: () -> Unit, onImport: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { PieIcon(PieGlyph.Previous, "กลับ") }
                Text("Google Drive", style = MaterialTheme.typography.titleLarge)
            }
        },
        bottomBar = {
            Surface(color = colors.background) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = onExport,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = RoundedCornerShape(28.dp),
                    ) { Text("สำรองไป Google Drive") }
                    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("ไว้ทีหลัง", color = colors.onSurfaceVariant)
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Surface(color = colors.primaryContainer, shape = RoundedCornerShape(28.dp)) {
                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    PieIcon(PieGlyph.CloudBackup, color = colors.primary, size = 36.dp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "สำรองข้อมูล\nบน Google Drive",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.semantics { heading() },
                )
                Text("เก็บสำเนาเรื่องเล่าของคุณ\nพร้อมนำกลับมาเมื่อเปลี่ยนเครื่อง", color = colors.onSurfaceVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DriveDetail(PieGlyph.Archive, "ข้อความ รูป และวิดีโอ", "รวมรูปแบบข้อความที่คุณจัดไว้")
                DriveDetail(PieGlyph.Lock, "เข้ารหัสก่อนอัปโหลด", "ใช้รหัสผ่านสำรองข้อมูล\nแยกจาก PIN เปิดแอป")
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ตั้งรหัสผ่าน แล้วเลือก Drive", style = MaterialTheme.typography.titleMedium)
                Text(
                    "ในหน้าบันทึกไฟล์ เปิดเมนูที่เก็บแล้วเลือก Google Drive ของคุณ หากไม่พบ ให้ติดตั้งแอป Drive และลงชื่อเข้าใช้ก่อน",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text("เลือกสำรองเองทุกครั้ง · ยังไม่ซิงก์อัตโนมัติ", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                TextButton(onClick = onImport, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("นำไฟล์จาก Drive กลับมา")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DriveDetail(glyph: PieGlyph, title: String, detail: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PieIcon(glyph, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
