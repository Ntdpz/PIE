package com.ntdpz.pie.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import com.ntdpz.pie.core.designsystem.theme.CompanionColors
import com.ntdpz.pie.core.designsystem.theme.LocalPieDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ntdpz.pie.BuildConfig
import com.ntdpz.pie.PiePetOverlayService
import com.ntdpz.pie.feature.pet.overlay.PetOverlayControls
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import com.ntdpz.pie.di.AppContainer
import com.ntdpz.pie.feature.editor.EditorRoute
import com.ntdpz.pie.feature.editor.EditorViewModel
import com.ntdpz.pie.feature.history.HistoryRoute
import com.ntdpz.pie.feature.history.HistoryViewModel
import com.ntdpz.pie.feature.home.HomeRoute
import com.ntdpz.pie.feature.home.HomeViewModel
import com.ntdpz.pie.feature.settings.SettingsRoute
import com.ntdpz.pie.feature.settings.SettingsViewModel
import com.ntdpz.pie.feature.settings.SettingsAction
import com.ntdpz.pie.feature.settings.drive.DriveScreen
import com.ntdpz.pie.feature.settings.reminder.ReminderRoute
import com.ntdpz.pie.feature.settings.pin.PinRoute
import com.ntdpz.pie.feature.settings.pin.PinPurpose
import com.ntdpz.pie.feature.settings.pin.PinViewModel
import com.ntdpz.pie.feature.settings.backup.BackupRoute
import com.ntdpz.pie.feature.settings.backup.BackupViewModel
import com.ntdpz.pie.feature.pet.PetCompanionRoute
import com.ntdpz.pie.feature.pet.PetSessionLifecycle
import com.ntdpz.pie.feature.pet.PetSettingsRoute
import com.ntdpz.pie.feature.pet.PetViewModel
import com.ntdpz.pie.feature.pet.PetInteractionSurface
import java.time.LocalDate

@Composable
fun PieApp(container: AppContainer, unlocked: Boolean = true) {
    val pet: PetViewModel = viewModel(factory = viewModelFactory {
        initializer {
            PetViewModel(
                container.petRepository, container.diaryWriter.states, container.clock,
                android.os.SystemClock::elapsedRealtime, container.applicationScope,
            )
        }
    })
    PetSessionLifecycle(pet, unlocked)
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val openDiary: (LocalDate) -> Unit = { date -> nav.navigate("editor/$date") }
    PetInteractionSurface(pet, enabled = unlocked && PieDestination.entries.any { it.route == route }) { onPetBounds ->
        Scaffold(
            containerColor = if (route == "pet") {
                if (LocalPieDarkTheme.current) CompanionColors.Ink else CompanionColors.Cream
            } else MaterialTheme.colorScheme.background,
            bottomBar = {
                if (PieDestination.entries.any { it.route == route }) {
                    PieBottomBar(route) { destination ->
                        nav.navigate(destination.route) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                NavHost(
                    navController = nav,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        val direction = navigationDirection(initialState.destination.route, targetState.destination.route)
                        fadeIn(tween(PieMotion.PageMillis)) +
                            slideInHorizontally(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized)) { it / 10 * direction } +
                            scaleIn(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized), initialScale = 0.985f)
                    },
                    exitTransition = {
                        val direction = navigationDirection(initialState.destination.route, targetState.destination.route)
                        fadeOut(tween(PieMotion.QuickMillis)) +
                            slideOutHorizontally(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized)) { -it / 16 * direction }
                    },
                    popEnterTransition = {
                        fadeIn(tween(PieMotion.PageMillis)) +
                            slideInHorizontally(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized)) { -it / 10 }
                    },
                    popExitTransition = {
                        fadeOut(tween(PieMotion.QuickMillis)) +
                            slideOutHorizontally(tween(PieMotion.PageMillis, easing = PieMotion.Emphasized)) { it / 10 }
                    },
                ) {
                    composable("home") {
                        val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                            initializer { HomeViewModel(container.diaryRepository, container.clock, container.mediaRepository) }
                        })
                        HomeRoute(vm, openDiary)
                    }
                    composable("history") {
                        val vm: HistoryViewModel = viewModel(factory = viewModelFactory {
                            initializer { HistoryViewModel(container.diaryRepository, container.clock, container.mediaRepository) }
                        })
                        HistoryRoute(vm, openDiary) { date -> nav.navigate("memory/$date") }
                    }
                    composable("memory/{date}", arguments = listOf(navArgument("date") { type = NavType.StringType })) { entry ->
                        val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
                        val vm: com.ntdpz.pie.feature.history.MemoryViewModel = viewModel(factory = viewModelFactory {
                            initializer { com.ntdpz.pie.feature.history.MemoryViewModel(container.diaryRepository, container.mediaRepository, date) }
                        })
                        com.ntdpz.pie.feature.history.MemoryRoute(vm, { nav.popBackStack() }, openDiary)
                    }
                    composable("settings") {
                        val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                            initializer { SettingsViewModel(container.settingsRepository, container.backupRepository, container.applicationScope, container.mergeImportRepository) }
                        })
                        SettingsRoute(vm, BuildConfig.VERSION_NAME) { action ->
                            nav.navigate(when (action) {
                                SettingsAction.SetupPin -> "pin/setup"
                                SettingsAction.ChangePin -> "pin/change"
                                SettingsAction.DisablePin -> "pin/disable"
                                SettingsAction.Reminders -> "reminders"
                                SettingsAction.ExportBackup -> "backup/export"
                                SettingsAction.RestoreBackup -> "backup/restore"
                                SettingsAction.GoogleDrive -> "drive"
                                SettingsAction.Pet -> "pet"
                            })
                        }
                    }
                    composable("drive") {
                        DriveScreen(
                            onBack = { nav.popBackStack() },
                            onExport = { nav.navigate("backup/drive") },
                            onImport = { nav.navigate("backup/restore") },
                        )
                    }
                    composable("pet") {
                        PetSettingsRoute(pet, onBack = { nav.popBackStack() }, motionAllowed = unlocked) {
                            PetOverlayControls(PiePetOverlayService::class.java, container.petOverlayStatus)
                        }
                    }
                    composable("reminders") {
                        val vm: SettingsViewModel = viewModel(factory = viewModelFactory {
                            initializer { SettingsViewModel(container.settingsRepository, container.backupRepository, container.applicationScope, container.mergeImportRepository) }
                        })
                        ReminderRoute(vm, LocalDate.now(container.clock), { nav.popBackStack() })
                    }
                    composable("pin/{purpose}") { entry ->
                        val purpose = when (entry.arguments?.getString("purpose")) {
                            "change" -> PinPurpose.Change
                            "disable" -> PinPurpose.Disable
                            else -> PinPurpose.Setup
                        }
                        val vm: PinViewModel = viewModel(factory = viewModelFactory {
                            initializer { PinViewModel(container.settingsRepository, purpose) }
                        })
                        PinRoute(vm, onComplete = { nav.popBackStack() }, onBack = { nav.popBackStack() })
                    }
                    composable("backup/{mode}") { entry ->
                        val vm: BackupViewModel = viewModel(factory = viewModelFactory {
                            initializer { BackupViewModel(container.backupRepository, container.diaryWriter, container.mergeImportRepository, container.applicationScope) }
                        })
                        BackupRoute(
                            vm,
                            restoring = entry.arguments?.getString("mode") == "restore",
                            driveDestination = entry.arguments?.getString("mode") == "drive",
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        route = "editor/{date}",
                        arguments = listOf(navArgument("date") { type = NavType.StringType }),
                    ) { entry ->
                        val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
                        DisposableEffect(date) {
                            pet.beginEditing(date)
                            onDispose { pet.finishEditing(date) }
                        }
                        val vm: EditorViewModel = viewModel(factory = viewModelFactory {
                            initializer { EditorViewModel(date, container.diaryRepository, container.diaryWriter, container.mediaRepository, pet::mediaCommitted) }
                        })
                        EditorRoute(vm, onBack = { nav.popBackStack() })
                    }
                }
                PetCompanionRoute(
                    pet,
                    visible = unlocked && PieDestination.entries.any { it.route == route },
                    onSettings = { nav.navigate("pet") },
                    onBoundsChanged = onPetBounds,
                )
            }
        }
    }
}
