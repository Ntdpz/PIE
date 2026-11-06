package com.ntdpz.pie.feature.pet

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ntdpz.pie.core.designsystem.theme.CompanionTheme
import com.ntdpz.pie.core.designsystem.component.CompanionSpriteGroup
import com.ntdpz.pie.core.domain.pet.PetPosition
import com.ntdpz.pie.core.domain.pet.PetReaction
import com.ntdpz.pie.core.domain.pet.PetEdge
import kotlinx.coroutines.delay

internal val PreviewActions = listOf(
    PetReaction.Wink, PetReaction.EarsUp, PetReaction.HeadTilt,
    PetReaction.Nod, PetReaction.Shy, PetReaction.Cheer,
    PetReaction.Stretch, PetReaction.Peekaboo, PetReaction.Heart,
)

@Composable
fun PetSettingsRoute(
    viewModel: PetViewModel,
    onBack: () -> Unit,
    motionAllowed: Boolean = true,
    outsideAppControls: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    var allPoses by rememberSaveable { mutableStateOf(false) }
    var selected by rememberSaveable { mutableStateOf(PetReaction.Wink) }
    var previewEdge by rememberSaveable { mutableStateOf<PetEdge?>(null) }
    var heroReaction by remember { mutableStateOf<PetReaction?>(null) }
    var replayKey by remember { mutableLongStateOf(0L) }
    val motion = motionAllowed && lifecycle.isAtLeast(Lifecycle.State.RESUMED) && !state.preferences.reducedMotion
    LaunchedEffect(heroReaction, replayKey) {
        heroReaction?.let { reaction ->
            delay((0 until 8).sumOf { reaction.animation().frameDuration(it) }.toLong())
            heroReaction = null
        }
    }
    BackHandler(allPoses) { allPoses = false }
    CompanionTheme {
        CompanionSpriteGroup {
            if (allPoses) {
                PetPoseScreen(
                    selected = selected, replayKey = replayKey, motionEnabled = motion,
                    onSelect = { selected = it; previewEdge = null; replayKey++ }, onBack = { allPoses = false },
                    edge = previewEdge, onEdge = { previewEdge = it; replayKey++ },
                )
            } else {
                PetSettingsScreen(
                    state = state, preview = heroReaction, replayKey = replayKey, motionEnabled = motion,
                    onBack = onBack, onEnabled = viewModel::setEnabled,
                    onReducedMotion = viewModel::setReducedMotion,
                    onReset = { viewModel.setPosition(PetPosition()) }, onRetry = viewModel::reload,
                    onTap = { heroReaction = PreviewActions[(replayKey % PreviewActions.size).toInt()]; replayKey++ },
                    onPreview = { selected = it; previewEdge = null; replayKey++; allPoses = true },
                    onAllPoses = { allPoses = true }, outsideAppControls = outsideAppControls,
                )
            }
        }
    }
}
