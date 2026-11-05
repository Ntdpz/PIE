package com.ntdpz.pie.feature.editor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.SpanStyle
import com.ntdpz.pie.core.domain.diary.TextFormat
import com.ntdpz.pie.core.domain.diary.TextFormatRange
import com.ntdpz.pie.core.domain.diary.TextFormatting
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
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
    onDocumentChanged: (String, List<TextFormatRange>) -> Unit = { text, _ -> onTextChanged(text) },
) {
    var field by rememberSaveable(state.date, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var formatting by remember(state.date) { mutableStateOf(state.formatting) }
    var formatOpen by remember { mutableStateOf(false) }
    var typingFormat by remember { mutableStateOf<TextFormat?>(null) }
    var voiceStart by remember { mutableStateOf<EditorDocument?>(null) }
    var voiceUndo by remember { mutableStateOf<Pair<EditorDocument, EditorDocument>?>(null) }
    val context = LocalContext.current
    val editable = !state.loading && !state.loadFailed && !state.deleting && !state.deleted
    val currentEditable by rememberUpdatedState(editable)
    fun applyDocument(document: EditorDocument) {
        field = document.value
        formatting = document.formatting
        onDocumentChanged(field.text, formatting)
    }
    val onVoiceResult by rememberUpdatedState<(String) -> Unit>({ text ->
        val original = voiceStart
        if (currentEditable && original != null && original.value.text == field.text && original.formatting == formatting) {
            val inserted = replaceSelection(original, text)
            applyDocument(inserted)
            voiceUndo = original to inserted
        }
        voiceStart = null
    })
    val voice = remember(state.date, context) { OnDeviceDictation(context) { onVoiceResult(it) } }
    DisposableEffect(voice) { onDispose { voice.cancel() } }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && currentEditable) {
            voiceStart = EditorDocument(field, formatting)
            voice.start()
        } else if (!granted) voice.permissionDenied()
    }
    LaunchedEffect(voice.active, voice.finishing) {
        if (voice.active && voice.finishing) {
            delay(8_000)
            voice.timedOut()
            voiceStart = null
        }
    }
    LaunchedEffect(editable) { if (!editable) { voice.cancel(); voiceStart = null } }
    fun startVoice() {
        formatOpen = false
        voiceUndo = null
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            voiceStart = EditorDocument(field, formatting)
            voice.start()
        }
    }
    fun changeFormat(format: TextFormat?) {
        voiceUndo = null
        val document = EditorDocument(field, formatting)
        if (paragraphSelection(field).collapsed) {
            typingFormat = format
        } else {
            typingFormat = null
            applyDocument(formatDocument(document, format))
        }
    }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var emojiPicker by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        menu = false
        confirmDelete = false
        emojiPicker = false
        formatOpen = false
        voice.cancel()
        voiceStart = null
    }
    LaunchedEffect(state.date, state.text, state.formatting) {
        formatting = state.formatting
        if (field.text != state.text) {
            val selection = if (field.text.isEmpty()) {
                TextRange(state.text.length)
            } else {
                TextRange(
                    field.selection.start.coerceAtMost(state.text.length),
                    field.selection.end.coerceAtMost(state.text.length),
                )
            }
            field = TextFieldValue(state.text, selection)
        }
    }
    fun insert(token: String) {
        voiceUndo = null
        applyDocument(replaceSelection(EditorDocument(field, formatting), token))
    }
    val voiceColor = MaterialTheme.colorScheme.primary
    val display = if (voice.active && voice.partial.isNotBlank()) {
        voiceStart?.let { replaceSelection(it, voice.partial) } ?: EditorDocument(field, formatting)
    } else EditorDocument(field, formatting)
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
                if (voice.active) Text("ข้อความจากเสียงจะบันทึกเมื่อพูดจบ", style = MaterialTheme.typography.labelSmall)
                else SaveStatus(state.saveState, onRetrySave)
            }
            Box {
                IconButton(onClick = { menu = true }, Modifier.size(48.dp)) { PieIcon(PieGlyph.More, "ตัวเลือกบันทึก") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("เพิ่ม #") },
                        onClick = { menu = false; insert("#") },
                        enabled = editable && !voice.active,
                        leadingIcon = { PieIcon(PieGlyph.Hash) },
                    )
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
            val fieldHeight = if (state.media.isEmpty()) (maxHeight - 48.dp).coerceAtLeast(0.dp) else (maxHeight - 220.dp).coerceAtLeast(180.dp)
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
                        value = display.value,
                        onValueChange = { value ->
                            if (value.text != field.text) {
                                voiceUndo = null
                                var updated = TextFormatting.afterEdit(field.text, value.text, formatting, extendAtEnd = true)
                                typingFormat?.let { format ->
                                    val prefix = field.text.commonPrefixWith(value.text).length
                                    val suffix = field.text.drop(prefix).commonSuffixWith(value.text.drop(prefix)).length
                                    val end = value.text.length - suffix
                                    if (end > prefix) {
                                        updated = TextFormatting.normalize(value.text, updated + TextFormatRange(prefix, end, format))
                                    }
                                }
                                applyDocument(EditorDocument(value, updated))
                            } else {
                                if (value.selection != field.selection) typingFormat = null
                                field = value
                            }
                        },
                        visualTransformation = VisualTransformation { source ->
                            val styled = styledEditorText(source.text, display.formatting)
                            val annotated = if (voice.active && voice.partial.isNotBlank()) {
                                androidx.compose.ui.text.AnnotatedString.Builder(styled).apply {
                                    val start = voiceStart?.value?.selection?.min ?: 0
                                    addStyle(SpanStyle(color = voiceColor), start, (start + voice.partial.length).coerceAtMost(source.text.length))
                                }.toAnnotatedString()
                            } else styled
                            TransformedText(annotated, OffsetMapping.Identity)
                        },
                        readOnly = voice.active,
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
        AnimatedVisibility(
            visible = formatOpen && !voice.active,
            enter = fadeIn(tween(PieMotion.QuickMillis)) + expandVertically(tween(PieMotion.GentleMillis)),
            exit = fadeOut(tween(PieMotion.QuickMillis)) + shrinkVertically(tween(PieMotion.GentleMillis)),
        ) {
            val paragraph = paragraphSelection(field)
            val range = if (field.selection.collapsed) paragraph else field.selection
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    FormatButton(PieGlyph.Heading, "หัวข้อ", editable,
                        TextFormatting.covers(formatting, paragraph.min, paragraph.max, TextFormat.Heading) || typingFormat == TextFormat.Heading,
                    ) { changeFormat(TextFormat.Heading) }
                    FormatButton(PieGlyph.TextNormal, "ข้อความปกติ", editable, false) { changeFormat(null) }
                    FormatButton(PieGlyph.Bold, "ตัวหนา", editable,
                        TextFormatting.covers(formatting, range.min, range.max, TextFormat.Bold) || typingFormat == TextFormat.Bold,
                    ) { changeFormat(TextFormat.Bold) }
                    FormatButton(PieGlyph.List, "รายการ", editable, false) {
                        voiceUndo = null
                        applyDocument(toggleBullets(EditorDocument(field, formatting)))
                    }
                    FormatButton(PieGlyph.Check, "ปิดแผงจัดรูปแบบ", true, false) { formatOpen = false }
                }
            }
        }
        voice.message?.let {
            Text(it, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (voice.active) {
                    PieLoader(if (voice.finishing) "กำลังแปลงเสียง…" else "กำลังฟังภาษาไทย", modifier = Modifier.weight(1f), compact = true)
                    IconButton(onClick = { voice.cancel(); voiceStart = null }, modifier = Modifier.size(48.dp)) {
                        PieIcon(PieGlyph.Close, "ยกเลิกพิมพ์เสียง")
                    }
                    FilledIconButton(onClick = voice::stop, enabled = !voice.finishing, modifier = Modifier.size(48.dp)) {
                        PieIcon(PieGlyph.Stop, "หยุดพูด", color = MaterialTheme.colorScheme.onPrimary)
                    }
                } else if (voiceUndo != null) {
                    Text("แทรกข้อความแล้ว", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    IconButton(
                        onClick = {
                            voiceUndo?.let { (before, after) ->
                                if (field.text == after.value.text && formatting == after.formatting) applyDocument(before)
                            }
                            voiceUndo = null
                        },
                        modifier = Modifier.size(48.dp),
                    ) { PieIcon(PieGlyph.Undo, "ย้อนการแทรกข้อความจากเสียง") }
                    IconButton(onClick = { voiceUndo = null }, modifier = Modifier.size(48.dp)) {
                        PieIcon(PieGlyph.Check, "เขียนต่อ")
                    }
                } else {
                    IconButton(onClick = onAddMedia, enabled = editable && !state.importing, modifier = Modifier.size(48.dp)) {
                        PieIcon(PieGlyph.Attach, "เพิ่มรูปหรือวิดีโอ")
                    }
                    FilledTonalIconButton(
                        onClick = { formatOpen = !formatOpen },
                        enabled = editable,
                        modifier = Modifier.size(48.dp).semantics {
                            contentDescription = "จัดรูปแบบข้อความ"
                            selected = formatOpen
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (formatOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ),
                    ) { Text("Aa", style = MaterialTheme.typography.titleMedium) }
                    IconButton(onClick = { emojiPicker = true }, enabled = editable, modifier = Modifier.size(48.dp)) {
                        PieIcon(PieGlyph.Emoji, "เพิ่ม Emoji")
                    }
                    Spacer(Modifier.weight(1f))
                    FilledIconButton(onClick = ::startVoice, enabled = editable, modifier = Modifier.size(48.dp)) {
                        PieIcon(PieGlyph.Mic, "พิมพ์ด้วยเสียง", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
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
private fun FormatButton(glyph: PieGlyph, label: String, enabled: Boolean, selected: Boolean, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp).semantics { this.selected = selected },
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) { PieIcon(glyph, label, color = MaterialTheme.colorScheme.onSurface) }
}
