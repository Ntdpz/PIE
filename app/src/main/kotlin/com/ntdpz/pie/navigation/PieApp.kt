package com.ntdpz.pie.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ntdpz.pie.core.designsystem.motion.PieMotion
import com.ntdpz.pie.di.AppContainer
import com.ntdpz.pie.feature.editor.EditorRoute
import com.ntdpz.pie.feature.editor.EditorViewModel
import com.ntdpz.pie.feature.history.HistoryRoute
import com.ntdpz.pie.feature.history.HistoryViewModel
import com.ntdpz.pie.feature.home.HomeRoute
import com.ntdpz.pie.feature.home.HomeViewModel
import com.ntdpz.pie.feature.settings.SettingsScreen
import java.time.LocalDate

@Composable
fun PieApp(container: AppContainer) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val openDiary: (LocalDate) -> Unit = { date -> nav.navigate("editor/$date") }
    Scaffold(
        bottomBar = {
            if (PieDestination.entries.any { it.route == route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    PieDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                nav.navigate(destination.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(PieMotion.PageMillis)) },
            exitTransition = { fadeOut(tween(PieMotion.QuickMillis)) },
        ) {
            composable("home") {
                val vm: HomeViewModel = viewModel(factory = viewModelFactory {
                    initializer { HomeViewModel(container.diaryRepository, container.clock) }
                })
                HomeRoute(vm, openDiary)
            }
            composable("history") {
                val vm: HistoryViewModel = viewModel(factory = viewModelFactory {
                    initializer { HistoryViewModel(container.diaryRepository, container.clock) }
                })
                HistoryRoute(vm, openDiary)
            }
            composable("settings") { SettingsScreen(BuildConfig.VERSION_NAME) }
            composable(
                route = "editor/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType }),
            ) { entry ->
                val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
                val vm: EditorViewModel = viewModel(factory = viewModelFactory {
                    initializer { EditorViewModel(date, container.diaryRepository, container.diaryWriter) }
                })
                EditorRoute(vm, onBack = { nav.popBackStack() })
            }
        }
    }
}
