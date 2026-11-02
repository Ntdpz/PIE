package com.ntdpz.pie.security

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ntdpz.pie.core.designsystem.component.PiePrimaryButton
import com.ntdpz.pie.core.designsystem.component.PieLoader
import com.ntdpz.pie.di.AppContainer
import com.ntdpz.pie.feature.settings.pin.*

@Composable
fun PieSessionGate(container: AppContainer, content: @Composable () -> Unit) {
    val session: SessionViewModel = viewModel(factory = viewModelFactory {
        initializer { SessionViewModel(container.settingsRepository) }
    })
    val pin: PinViewModel = viewModel(key = "session-pin", factory = viewModelFactory {
        initializer { PinViewModel(container.settingsRepository, PinPurpose.Unlock) }
    })
    val state by session.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    SideEffect {
        if (!state.settings.loaded || state.settings.pinEnabled) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    DisposableEffect(lifecycle, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isChangingConfigurations != true) {
                pin.restartUnlock()
                session.lock()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(state.locked && state.settings.pinEnabled) { activity?.moveTaskToBack(true) }
    if (!state.settings.loaded || state.error) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                if (state.error) {
                    Text("เปิดการตั้งค่าไม่สำเร็จ")
                    PiePrimaryButton("ลองอีกครั้ง", session::retry)
                } else PieLoader("กำลังเปิดพื้นที่ของคุณ…")
            }
        }
    } else Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(if (state.locked) Modifier.clearAndSetSemantics { } else Modifier)) { content() }
        if (state.locked) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.safeDrawingPadding()) { PinRoute(pin, session::unlock) }
        }
    }
}
