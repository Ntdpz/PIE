package com.ntdpz.pie.feature.pet

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/** Observes gesture starts without consuming events or duplicating the pet's own gestures. */
@Composable
fun PetInteractionSurface(
    viewModel: PetViewModel,
    enabled: Boolean,
    content: @Composable (onPetBounds: (Rect?) -> Unit) -> Unit,
) {
    var petBounds by remember { mutableStateOf<Rect?>(null) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val active by rememberUpdatedState(enabled)
    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() }
            .pointerInput(viewModel) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (active && petBounds?.contains(origin + down.position) != true) {
                        viewModel.screenTouched()
                    }
                }
            },
    ) {
        content { petBounds = it }
    }
}
