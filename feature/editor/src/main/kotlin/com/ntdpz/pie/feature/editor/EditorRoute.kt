package com.ntdpz.pie.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EditorRoute(viewModel: EditorViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        viewModel.importMedia(uris.map { it.toString() })
    }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    EditorScreen(
        state, viewModel::onTextChanged, viewModel::load, viewModel::retrySave, onBack,
        onAddMedia = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
        onDeleteEntry = viewModel::deleteEntry,
        onDeleteMedia = viewModel::deleteMedia,
        onDocumentChanged = viewModel::onDocumentChanged,
    )
}
