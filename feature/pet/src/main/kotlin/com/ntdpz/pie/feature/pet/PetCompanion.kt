package com.ntdpz.pie.feature.pet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ntdpz.pie.core.designsystem.component.CompanionAnimation
import com.ntdpz.pie.core.designsystem.component.CompanionSprite
import com.ntdpz.pie.core.designsystem.motion.rememberSystemAnimationScale
import com.ntdpz.pie.core.domain.pet.PetPose
import com.ntdpz.pie.core.domain.pet.PetPosition
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PetSessionLifecycle(viewModel: PetViewModel, unlocked: Boolean) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, viewModel, unlocked) {
        if (unlocked) owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.resume()
            try {
                while (true) {
                    viewModel.tick()
                    delay(250)
                }
            } finally {
                viewModel.pause()
            }
        }
    }
}

@Composable
fun PetCompanionRoute(
    viewModel: PetViewModel,
    visible: Boolean,
    onSettings: () -> Unit,
    onBoundsChanged: (Rect?) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    if (visible && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && state.loaded && state.preferences.enabled) {
        PetCompanion(
            state, viewModel::tap, viewModel::setPosition,
            onHide = { viewModel.setEnabled(false) }, onSettings = onSettings,
            onBoundsChanged = onBoundsChanged,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PetCompanion(
    state: PetUiState,
    onTap: () -> Unit,
    onPosition: (PetPosition) -> Unit,
    onHide: () -> Unit,
    onSettings: () -> Unit,
    onBoundsChanged: (Rect?) -> Unit = {},
) {
    val scale = rememberSystemAnimationScale()
    var menu by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(state.preferences.position) }
    var bubbleHeight by remember { mutableStateOf(0) }
    val savedPosition by rememberUpdatedState(state.preferences.position)
    val reportBounds by rememberUpdatedState(onBoundsChanged)
    DisposableEffect(Unit) { onDispose { reportBounds(null) } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { menu = false }
    LaunchedEffect(state.preferences.position) {
        if (!dragging) position = state.preferences.position
    }
    BoxWithConstraints(Modifier.fillMaxSize().testTag("pet-layer")) {
        val density = LocalDensity.current
        val width = with(density) { 128.dp.toPx() }
        val height = with(density) { 128.dp.toPx() }
        val maxX = (constraints.maxWidth - width).coerceAtLeast(0f)
        val maxY = (constraints.maxHeight - height).coerceAtLeast(0f)
        val x = position.x * maxX
        val y = position.y * maxY
        val bubbleWidth = minOf(maxWidth, 240.dp)
        val bubbleWidthPx = with(density) { bubbleWidth.toPx() }
        val bubbleX = (x + width - bubbleWidthPx).coerceIn(0f, (constraints.maxWidth - bubbleWidthPx).coerceAtLeast(0f))
        val gap = with(density) { 8.dp.toPx() }
        val desiredBubbleY = if (y >= bubbleHeight + gap) y - bubbleHeight - gap else y + height
        val bubbleY = desiredBubbleY.coerceIn(0f, (constraints.maxHeight - bubbleHeight).coerceAtLeast(0).toFloat())
        if (state.error != null || state.pose == PetPose.Greeting ||
            (state.pose == PetPose.Tap && state.showTouchMessage) || state.pose == PetPose.Saved
        ) {
            Surface(
                Modifier.offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }.width(bubbleWidth)
                    .heightIn(max = maxHeight).onSizeChanged { bubbleHeight = it.height }
                    .semantics { liveRegion = LiveRegionMode.Polite },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    state.error ?: if (state.pose == PetPose.Saved) "บันทึกเรียบร้อยแล้ว" else state.dailyMessage,
                    Modifier.verticalScroll(rememberScrollState()).padding(12.dp), style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Box(
            Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }.size(128.dp)
                .onGloballyPositioned { reportBounds(it.boundsInRoot()) }
                .testTag("pet-companion")
                .semantics(mergeDescendants = true) {
                    contentDescription = "PIE เพื่อนของคุณ"
                    stateDescription = when (state.pose) {
                        PetPose.Idle -> "อยู่เป็นเพื่อน"
                        PetPose.Tap -> state.touchReaction?.action?.label() ?: "ทักทาย"
                        PetPose.Greeting -> "ทักทาย"
                        PetPose.Saved -> "ดีใจกับบันทึกที่เก็บแล้ว"
                        PetPose.Rest -> "พัก"
                        PetPose.Stretch -> "ยืดเส้น"
                    }
                    customActions = listOf(
                        CustomAccessibilityAction("ซ่อน PIE") { onHide(); true },
                        CustomAccessibilityAction("ตั้งค่า PIE") { onSettings(); true },
                        CustomAccessibilityAction("ย้าย PIE ไปด้านซ้าย") {
                            onPosition(position.copy(x = (position.x - 0.2f).coerceAtLeast(0f))); true
                        },
                        CustomAccessibilityAction("ย้าย PIE ไปด้านขวา") {
                            onPosition(position.copy(x = (position.x + 0.2f).coerceAtMost(1f))); true
                        },
                        CustomAccessibilityAction("ย้าย PIE ขึ้น") {
                            onPosition(position.copy(y = (position.y - 0.2f).coerceAtLeast(0f))); true
                        },
                        CustomAccessibilityAction("ย้าย PIE ลง") {
                            onPosition(position.copy(y = (position.y + 0.2f).coerceAtMost(1f))); true
                        },
                    )
                }
                .combinedClickable(role = Role.Button, onClick = onTap, onLongClick = { menu = true })
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { dragging = true; onTap() },
                        onDragEnd = { onPosition(position); dragging = false },
                        onDragCancel = { position = savedPosition; dragging = false },
                    ) { change, amount ->
                        change.consume()
                        position = PetPosition(
                            if (maxX > 0) ((position.x * maxX + amount.x) / maxX).coerceIn(0f, 1f) else 0f,
                            if (maxY > 0) ((position.y * maxY + amount.y) / maxY).coerceIn(0f, 1f) else 0f,
                        )
                    }
                },
        ) {
            CompanionSprite(
                animation = when (state.pose) {
                    PetPose.Idle -> CompanionAnimation.Idle
                    PetPose.Tap -> state.touchReaction?.action?.animation() ?: CompanionAnimation.Tap
                    PetPose.Greeting -> CompanionAnimation.Greeting
                    PetPose.Saved -> CompanionAnimation.Saved
                    PetPose.Rest -> CompanionAnimation.Rest
                    PetPose.Stretch -> CompanionAnimation.Stretch
                },
                motionEnabled = !state.preferences.reducedMotion && !dragging,
                modifier = Modifier.fillMaxSize(),
                durationScale = scale,
                replayKey = state.touchReaction?.sequence ?: 0,
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("ตั้งค่า PIE") }, onClick = { menu = false; onSettings() })
                DropdownMenuItem(text = { Text("ซ่อน PIE") }, onClick = { menu = false; onHide() })
            }
        }
    }
}
