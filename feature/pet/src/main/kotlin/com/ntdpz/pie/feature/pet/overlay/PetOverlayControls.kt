package com.ntdpz.pie.feature.pet.overlay

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Android permission/service boundary. No Android objects enter the ViewModel. */
@Composable
fun PetOverlayControls(serviceClass: Class<out PetOverlayService>) {
    val context = LocalContext.current
    var allowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var message by remember { mutableStateOf<String?>(null) }
    val start = {
        try {
            context.startForegroundService(Intent(context, serviceClass))
            message = "ส่งคำขอเปิดแล้ว กลับหน้าจอหลักเพื่อพบน้อง ดูสถานะและหยุดได้จากแจ้งเตือน PIE"
        } catch (_: Exception) {
            message = "เริ่ม PIE นอกแอปไม่ได้ ลองเปิดจากหน้านี้อีกครั้ง"
        }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start()
        else message = "กรุณาอนุญาตแจ้งเตือนให้ PIE เพื่อแสดงสถานะและปุ่มหยุด PET แล้วลองเปิดอีกครั้ง"
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = Settings.canDrawOverlays(context)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PIE นอกแอป")
        Text("ลอยบนหน้าจอหลักและแอปอื่น ตอบสนองเมื่อเริ่มแตะหน้าจอ ลากตัวน้องได้ กดค้างเพื่อปิด น้องจะหลบเมื่อกลับเข้า PIE หรือดับจอ")
        if (!allowed) {
            Button(onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                } catch (_: Exception) {
                    message = "เปิดหน้าสิทธิ์ไม่ได้ กรุณาเปิดแสดงทับแอปอื่นให้ PIE ในการตั้งค่าเครื่อง"
                }
            }) { Text("อนุญาตให้แสดงทับแอปอื่น") }
        }
        Button(enabled = allowed, onClick = {
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else start()
        }) { Text("เปิด PIE นอกแอป") }
        TextButton(onClick = {
            context.stopService(Intent(context, serviceClass))
            message = "ปิด PIE นอกแอปแล้ว"
        }) { Text("ปิด PIE นอกแอป") }
        Text("เปิดใหม่จากหน้านี้หลังรีสตาร์ตเครื่องหรือปิดบริการ โหมดนี้ใช้แบตเตอรี่เพิ่ม และไม่แสดงเนื้อหาบันทึก")
        message?.let { Text(it) }
    }
}
