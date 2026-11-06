package com.ntdpz.pie.feature.pet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntdpz.pie.core.designsystem.component.CompanionAnimation
import com.ntdpz.pie.core.designsystem.component.CompanionSprite
import com.ntdpz.pie.core.designsystem.component.PieGlyph
import com.ntdpz.pie.core.designsystem.component.PieIcon
import com.ntdpz.pie.core.designsystem.motion.rememberSystemAnimationScale
import com.ntdpz.pie.core.domain.pet.PetReaction
import com.ntdpz.pie.core.domain.pet.PetEdge
import com.ntdpz.pie.core.designsystem.component.CompanionEdge

@Composable
internal fun PetSettingsScreen(
    state: PetUiState,
    preview: PetReaction?,
    replayKey: Long,
    motionEnabled: Boolean,
    onBack: () -> Unit,
    onEnabled: (Boolean) -> Unit,
    onReducedMotion: (Boolean) -> Unit,
    onReset: () -> Unit,
    onRetry: () -> Unit,
    onTap: () -> Unit,
    onPreview: (PetReaction) -> Unit,
    onAllPoses: () -> Unit,
    outsideAppControls: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        CompanionHeader("เพื่อนของฉัน", onBack)
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 208.dp).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PIE", fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    Text("อยู่เป็นเพื่อนนะ", style = MaterialTheme.typography.bodyMedium)
                    Text("แตะน้องเพื่อทักทาย", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = onReset, enabled = state.loaded,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("pet-reset-position"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ), shape = RoundedCornerShape(16.dp),
                    ) { Text("คืนตำแหน่ง") }
                }
                Box(
                    Modifier.width(148.dp).height(176.dp).clickable(
                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                        role = Role.Button, onClickLabel = "ทักทาย PIE", onClick = onTap,
                    ).testTag("pet-hero").semantics { contentDescription = "PIE แตะเพื่อทักทาย" },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    CompanionSprite(
                        preview?.animation() ?: CompanionAnimation.Idle, motionEnabled,
                        Modifier.size(148.dp, 176.dp), durationScale = rememberSystemAnimationScale(),
                        replayKey = replayKey, characterScale = 1.5f,
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 24.dp)) {
            CompanionSwitch(
                "แสดง PIE ในแอป", "ตอบสนองเมื่อแตะหน้าจอ", state.preferences.enabled,
                state.loaded, onEnabled, "pet-enabled",
            )
            CompanionSwitch(
                "ลดการเคลื่อนไหว", "ใช้ท่านิ่งแทนแอนิเมชัน", state.preferences.reducedMotion,
                state.loaded, onReducedMotion, "pet-reduced-motion",
            )
            if (!state.loaded && state.error == null) Text("กำลังเปิดการตั้งค่า…")
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                if (!state.loaded) TextButton(onClick = onRetry) { Text("ลองอีกครั้ง") }
            }
            Spacer(Modifier.height(16.dp))
            outsideAppControls()
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ลองท่าของน้อง", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onAllPoses, modifier = Modifier.heightIn(min = 48.dp).testTag("pet-all-poses")) {
                    Text("ดูทุกท่า", color = MaterialTheme.colorScheme.onSurface)
                    PieIcon(PieGlyph.Next, size = 18.dp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(PetReaction.Wink, PetReaction.Shy, PetReaction.Heart).forEach { reaction ->
                    PoseTile(reaction, false, { onPreview(reaction) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompanionHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onBack, Modifier.size(48.dp)) { PieIcon(PieGlyph.Back, "กลับ") }
        Text(title, Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompanionSwitch(
    title: String, description: String, checked: Boolean, enabled: Boolean,
    onChange: (Boolean) -> Unit, tag: String,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked, onChange, Modifier.testTag(tag).semantics { contentDescription = title }, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                uncheckedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedBorderColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun PetPoseScreen(
    selected: PetReaction, replayKey: Long, motionEnabled: Boolean,
    onSelect: (PetReaction) -> Unit, onBack: () -> Unit,
    edge: PetEdge?, onEdge: (PetEdge?) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
    ) {
        CompanionHeader("ลองท่าของ PIE", onBack)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 224.dp).padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompanionSprite(
                        if (edge == null) selected.animation() else CompanionAnimation.Idle, motionEnabled,
                        Modifier.size(184.dp)
                            .then(if (edge != null) Modifier.border(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .3f)) else Modifier)
                            .testTag("pet-reaction-preview")
                            .semantics { contentDescription = "ตัวอย่าง PIE · ${edge?.label() ?: selected.label()}" },
                        durationScale = rememberSystemAnimationScale(), replayKey = replayKey,
                        edge = edge?.visual() ?: CompanionEdge.Center,
                    )
                    Text(edge?.label() ?: selected.label(), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    { onEdge(null) }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("pet-mode-reactions"),
                    border = BorderStroke(if (edge == null) 2.dp else 1.dp, MaterialTheme.colorScheme.primary),
                ) { Text("ทักทาย 9 ท่า") }
                OutlinedButton(
                    { onEdge(PetEdge.Right) }, Modifier.weight(1f).heightIn(min = 48.dp).testTag("pet-mode-edges"),
                    border = BorderStroke(if (edge != null) 2.dp else 1.dp, MaterialTheme.colorScheme.primary),
                ) { Text("เกาะขอบ 8 ท่า") }
            }
            Text(if (edge == null) "เลือกท่าที่อยากลอง" else "เลือกขอบหรือมุมที่อยากลอง", style = MaterialTheme.typography.bodyLarge)
            if (edge != null) {
                PetEdge.entries.filter { it != PetEdge.Center }.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { item -> EdgePoseTile(item, edge == item, { onEdge(item) }, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Text("ลากน้องไปชิดขอบจอเพื่อใช้ท่านี้จริง", style = MaterialTheme.typography.bodySmall)
            } else PreviewActions.chunked(3).forEach { row ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { reaction ->
                        PoseTile(reaction, selected == reaction, { onSelect(reaction) }, Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "ลองเล่นได้ ไม่เปลี่ยนการตั้งค่า", Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onBack, Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("pet-poses-back"),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) { Text("กลับไปหา PIE", color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

@Composable
private fun EdgePoseTile(edge: PetEdge, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.testTag("pet-preview-edge-${edge.name}").selectable(
            selected, interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.RadioButton, onClick = onClick,
        ),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompanionSprite(CompanionAnimation.Idle, false, Modifier.size(72.dp), edge = edge.visual())
            Text(edge.label(), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PoseTile(reaction: PetReaction, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.testTag("pet-preview-${reaction.name}").selectable(
            selected, interactionSource = remember { MutableInteractionSource() }, indication = null,
            role = Role.RadioButton, onClick = onClick,
        ),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 104.dp).padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompanionSprite(reaction.animation(), false, Modifier.size(80.dp))
            Text(reaction.label(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
