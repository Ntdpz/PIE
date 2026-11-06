package com.ntdpz.pie.feature.pet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.ntdpz.pie.core.designsystem.component.CompanionAnimation
import com.ntdpz.pie.core.designsystem.component.CompanionSprite
import com.ntdpz.pie.core.designsystem.component.PieGlyph
import com.ntdpz.pie.core.designsystem.component.PieIcon
import com.ntdpz.pie.core.designsystem.motion.rememberSystemAnimationScale
import com.ntdpz.pie.core.domain.pet.PetPosition

@Composable
fun PetSettingsRoute(
    viewModel: PetViewModel,
    onBack: () -> Unit,
    motionAllowed: Boolean = true,
    outsideAppControls: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { PieIcon(PieGlyph.Back, "กลับ") }
            Text("เพื่อนของฉัน · PIE", style = MaterialTheme.typography.headlineSmall)
        }
        CompanionSprite(
            CompanionAnimation.Idle,
            motionEnabled = motionAllowed && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !state.preferences.reducedMotion,
            modifier = Modifier.size(160.dp, 174.dp).align(Alignment.CenterHorizontally),
            durationScale = rememberSystemAnimationScale(),
        )
        Text("ลาก PIE ไปไว้ตรงที่ต้องการ แตะเพื่อทักทาย หรือกดค้างเพื่อซ่อนและตั้งค่า")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("แสดง PIE ในแอป", Modifier.weight(1f))
            Switch(
                state.preferences.enabled, viewModel::setEnabled,
                Modifier.testTag("pet-enabled"), enabled = state.loaded,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("ลดการเคลื่อนไหว", Modifier.weight(1f))
            Switch(
                state.preferences.reducedMotion, viewModel::setReducedMotion,
                Modifier.testTag("pet-reduced-motion"), enabled = state.loaded,
            )
        }
        TextButton(onClick = { viewModel.setPosition(PetPosition()) }, enabled = state.loaded) {
            Text("คืนตำแหน่งเริ่มต้น")
        }
        if (!state.loaded && state.error == null) Text("กำลังเปิดการตั้งค่า…")
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            if (!state.loaded) TextButton(onClick = viewModel::reload) { Text("ลองอีกครั้ง") }
        }
        outsideAppControls()
        Text("PIE ในแอปจะหลบระหว่างเขียนหรือปลดล็อก PIN", style = MaterialTheme.typography.bodySmall)
    }
}
