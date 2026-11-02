package com.ntdpz.pie.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ntdpz.pie.core.domain.diary.SaveState
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: EditorUiState,
    onTextChanged: (String) -> Unit,
    onRetryLoad: () -> Unit,
    onRetrySave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.date.format(
                            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("th")),
                        ))
                        SaveStatus(state.saveState, onRetrySave)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "กลับ")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.padding(24.dp))
                state.loadFailed -> TextButton(onClick = onRetryLoad) {
                    Text("เปิดบันทึกไม่สำเร็จ · ลองอีกครั้ง")
                }
                else -> TextField(
                    value = state.text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    placeholder = { Text("เริ่มจากเรื่องที่อยู่ในใจ…") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SaveStatus(state: SaveState, onRetry: () -> Unit) {
    when (state) {
        SaveState.Idle -> Text("บันทึกอัตโนมัติ", style = MaterialTheme.typography.labelSmall)
        is SaveState.Saving -> Text("กำลังบันทึก…", style = MaterialTheme.typography.labelSmall)
        is SaveState.Saved -> Text("บันทึกแล้ว", style = MaterialTheme.typography.labelSmall)
        is SaveState.Failed -> TextButton(onClick = onRetry) {
            Text("บันทึกไม่สำเร็จ · ลองอีกครั้ง", color = MaterialTheme.colorScheme.error)
        }
    }
}
