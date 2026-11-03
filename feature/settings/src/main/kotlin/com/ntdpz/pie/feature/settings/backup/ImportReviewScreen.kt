package com.ntdpz.pie.feature.settings.backup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.domain.backup.MergeChoice
import java.time.LocalDate

@Composable
internal fun ImportReviewScreen(
    state: BackupUiState,
    onChoice: (LocalDate, MergeChoice) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val preview = state.preview ?: return
    var step by rememberSaveable(preview.token) { mutableIntStateOf(0) }
    var conflictIndex by rememberSaveable(preview.token) { mutableIntStateOf(0) }
    val conflicts = preview.conflicts
    val back: () -> Unit = {
        if (step == 1 && conflictIndex > 0) conflictIndex--
        else if (step == 2 && conflicts.isNotEmpty()) step = 1
        else if (step > 0) step = 0
        else onCancel()
    }
    BackHandler { if (!state.busy) back() }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Surface {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    AnimatedVisibility(state.busy, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        PieLoader("กำลังรวมบันทึก…", compact = true)
                    }
                    PiePrimaryButton(
                        label = when (step) {
                            0 -> if (conflicts.isEmpty()) "ตรวจสรุปก่อนนำเข้า" else "เลือกข้อความ ${conflicts.size} วัน"
                            1 -> if (conflictIndex < conflicts.lastIndex) "ใช้ตัวเลือกนี้ · วันถัดไป" else "ใช้ตัวเลือกนี้ · ตรวจสรุป"
                            else -> "ยืนยันนำเข้า"
                        },
                        onClick = {
                            when (step) {
                                0 -> step = if (conflicts.isEmpty()) 2 else 1
                                1 -> if (conflictIndex < conflicts.lastIndex) conflictIndex++ else step = 2
                                else -> onConfirm()
                            }
                        },
                        enabled = !state.busy,
                    )
                    TextButton(
                        onClick = { if (step == 0) onCancel() else step = 0 },
                        enabled = !state.busy, modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (step == 0) "ยกเลิกและเลือกไฟล์ใหม่" else "กลับไปภาพรวม") }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TextButton(onClick = back, enabled = !state.busy, modifier = Modifier.padding(start = 12.dp)) {
                PieIcon(PieGlyph.Previous, size = 16.dp)
                Text(
                    when {
                        step == 0 -> "เลือกไฟล์สำรอง"
                        step == 1 && conflictIndex > 0 -> "วันก่อนหน้า"
                        step == 2 && conflicts.isNotEmpty() -> "เลือกข้อความ"
                        else -> "ภาพรวมรายการ"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            AnimatedContent(
                targetState = step to conflictIndex,
                transitionSpec = {
                    val forward = targetState.first > initialState.first ||
                        (targetState.first == initialState.first && targetState.second > initialState.second)
                    val direction = if (forward) 1 else -1
                    (fadeIn(tween(PieMotion.GentleMillis)) +
                        slideInHorizontally(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized)) { it / 8 * direction })
                        .togetherWith(fadeOut(tween(PieMotion.QuickMillis)) +
                            slideOutHorizontally(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized)) { -it / 12 * direction })
                },
                label = "Import step",
            ) { (page, index) ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    when (page) {
                        0 -> ImportOverview(preview)
                        1 -> ImportConflictContent(
                            conflicts[index], index, conflicts.size,
                            state.choices.getValue(conflicts[index].date), !state.busy,
                        ) { onChoice(conflicts[index].date, it) }
                        else -> ImportConfirmation(preview, state.choices)
                    }
                }
            }
        }
    }
}
