package com.ntdpz.pie.feature.pet.overlay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Android permission/service boundary. No Android objects enter the ViewModel. */
@Composable
fun PetOverlayControls(serviceClass: Class<out PetOverlayService>, status: PetOverlayStatus) {
    val context = LocalContext.current
    val state by status.state.collectAsStateWithLifecycle()
    var allowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val start = {
        status.starting()
        try {
            context.startForegroundService(Intent(context, serviceClass))
        } catch (_: Exception) {
            status.failed("เริ่ม PIE นอกแอปไม่ได้ ลองเปิดจากหน้านี้อีกครั้ง")
        }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start()
        else status.failed("กรุณาอนุญาตแจ้งเตือนให้ PIE เพื่อแสดงสถานะและปุ่มหยุด แล้วลองอีกครั้ง")
    }
    val requestStart = {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else start()
    }
    val overlayPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        allowed = Settings.canDrawOverlays(context)
        if (allowed) requestStart()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        allowed = Settings.canDrawOverlays(context)
    }
    val running = state.phase == PetOverlayPhase.Running
    val starting = state.phase == PetOverlayPhase.Starting
    val label = when (state.phase) {
        PetOverlayPhase.Off -> "ยังไม่เปิด"
        PetOverlayPhase.Starting -> "กำลังเปิด"
        PetOverlayPhase.Running -> "เปิดอยู่"
        PetOverlayPhase.Failed -> "เปิดไม่สำเร็จ"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("PIE นอกแอป", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp).testTag("pet-overlay-status")
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    if (running) "พร้อมอยู่ด้วยแล้ว · หยุดได้จากที่นี่หรือแจ้งเตือน" else "อยู่เป็นเพื่อนบนหน้าจออื่น",
                    style = MaterialTheme.typography.bodySmall,
                )
                val action: () -> Unit = {
                    if (running) {
                        try {
                            if (!context.stopService(Intent(context, serviceClass))) status.stopped()
                        } catch (_: Exception) {
                            status.failed("หยุด PIE ไม่สำเร็จ ลองหยุดจากแจ้งเตือน")
                        }
                    } else if (!allowed) {
                        try {
                            overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                        } catch (_: Exception) {
                            status.failed("เปิดหน้าสิทธิ์ไม่ได้ กรุณาเปิดแสดงทับแอปอื่นให้ PIE ในการตั้งค่าเครื่อง")
                        }
                    } else requestStart()
                }
                val actionLabel = when {
                    starting -> "กำลังเปิด PIE…"
                    running -> "หยุด PIE นอกแอป"
                    state.phase == PetOverlayPhase.Failed -> "ลองอีกครั้ง"
                    else -> "เปิด PIE นอกแอป"
                }
                if (running) {
                    OutlinedButton(
                        onClick = action,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("pet-overlay-action"),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(actionLabel, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                } else {
                    Button(
                        onClick = action, enabled = !starting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("pet-overlay-action"),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(actionLabel, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                }
                state.error?.let {
                    Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Text(
            if (!allowed) "เปิดครั้งแรกจะขอสิทธิ์แสดงทับแอปอื่น" else "น้องจะหลบเมื่อกลับเข้า PIE · กดค้างที่น้องเพื่อหยุด",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
