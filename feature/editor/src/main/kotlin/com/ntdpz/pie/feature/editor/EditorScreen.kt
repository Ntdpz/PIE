package com.ntdpz.pie.feature.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.designsystem.component.*
import com.ntdpz.pie.core.designsystem.format.PieDates
import com.ntdpz.pie.core.domain.diary.SaveState
import com.ntdpz.pie.feature.editor.media.MediaGallery
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onTextChanged: (String) -> Unit,
    onRetryLoad: () -> Unit,
    onRetrySave: () -> Unit,
    onBack: () -> Unit,
    onAddMedia: () -> Unit,
    onDeleteEntry: () -> Unit,
    onDeleteMedia: (String) -> Unit,
) {
    var field by rememberSaveable(state.date, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var emojiPicker by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        menu = false
        confirmDelete = false
        emojiPicker = false
    }
    LaunchedEffect(state.text) {
        if (field.text != state.text) field = TextFieldValue(state.text, TextRange(state.text.length))
    }
    fun insert(token: String) {
        val start = field.selection.min
        val text = field.text.replaceRange(start, field.selection.max, token)
        field = TextFieldValue(text, TextRange(start + token.length))
        onTextChanged(text)
    }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.ime)
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, Modifier.size(48.dp)) { PieIcon(PieGlyph.Back, "กลับ") }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(PieDates.entry(state.date), style = MaterialTheme.typography.titleMedium)
                SaveStatus(state.saveState, onRetrySave)
            }
            Box {
                IconButton(onClick = { menu = true }, Modifier.size(48.dp)) { PieIcon(PieGlyph.More, "ตัวเลือกบันทึก") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("ลบบันทึกนี้", color = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; confirmDelete = true },
                        enabled = !state.importing && !state.deleting,
                        leadingIcon = { PieIcon(PieGlyph.Trash, color = MaterialTheme.colorScheme.error) },
                    )
                }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val fieldHeight = if (state.media.isEmpty()) maxHeight - 48.dp else (maxHeight - 220.dp).coerceAtLeast(180.dp)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when {
                    state.loading -> Box(Modifier.fillMaxWidth().heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                        PieLoader("กำลังเปิดบันทึก…")
                    }
                    state.loadFailed -> TextButton(onClick = onRetryLoad) { Text("เปิดบันทึกไม่สำเร็จ · ลองอีกครั้ง") }
                    else -> BasicTextField(
                        value = field,
                        onValueChange = { field = it; onTextChanged(it.text) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = fieldHeight),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        enabled = !state.deleting,
                        decorationBox = { inner ->
                            Box {
                                if (field.text.isEmpty()) Text("เริ่มจากเรื่องที่อยู่ในใจ…", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                inner()
                            }
                        },
                    )
                }
                if (state.importing) PieLoader("กำลังเก็บรูปและวิดีโอ…", compact = true)
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                if (state.media.isNotEmpty()) MediaGallery(state.media, onDeleteMedia)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = onAddMedia,
                enabled = !state.loading && !state.importing && !state.deleting,
                modifier = Modifier.size(52.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(Modifier.padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PieIcon(PieGlyph.Media, "เพิ่มรูปหรือวิดีโอ", color = MaterialTheme.colorScheme.primary)

                }
            }
            ToolbarButton(PieGlyph.Emoji, "เพิ่ม Emoji", !state.loading) { emojiPicker = true }
            ToolbarButton(PieGlyph.Hash, "เพิ่ม #", !state.loading) { insert("#") }
        }
    }
    if (confirmDelete) PieDialog(
        onDismissRequest = { confirmDelete = false },
        title = "ลบบันทึกนี้?",
        text = { Text("ลบข้อความและสื่อของวันที่ ${PieDates.entry(state.date)}\nเมื่อลบแล้วจะกู้คืนไม่ได้") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDeleteEntry() }) { Text("ลบบันทึก", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("ยกเลิก") } },
    )
    if (emojiPicker) ModalBottomSheet(onDismissRequest = { emojiPicker = false }, containerColor = MaterialTheme.colorScheme.surface) {
        Text("เติมความรู้สึก", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.titleLarge)
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 6,
        ) {
            listOf("🙂", "🥹", "🥰", "😂", "😌", "😢", "😭", "😤", "🤍", "✨", "🌷", "🫶").forEach { emoji ->
                Surface(
                    onClick = { insert(emoji); emojiPicker = false },
                    modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(emoji, style = MaterialTheme.typography.headlineSmall) }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToolbarButton(glyph: PieGlyph, label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedIconButton(onClick, Modifier.size(52.dp), enabled, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        PieIcon(glyph, label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
