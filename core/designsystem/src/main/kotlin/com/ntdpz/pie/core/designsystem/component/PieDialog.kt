package com.ntdpz.pie.core.designsystem.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Shared paper, typography and scrolling for secondary decisions. */
@Composable
fun PieDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) { text() } },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}
