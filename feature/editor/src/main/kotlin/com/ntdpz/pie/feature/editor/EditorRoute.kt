package com.ntdpz.pie.feature.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditorRoute(viewModel: EditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EditorScreen(
        state = state,
        onTextChanged = viewModel::onTextChanged,
        onRetryLoad = viewModel::load,
        onRetrySave = viewModel::retrySave,
        onBack = onBack,
    )
}
