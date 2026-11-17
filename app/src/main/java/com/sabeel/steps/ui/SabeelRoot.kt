package com.sabeel.steps.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sabeel.steps.AppContainer
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.ui.components.LoadingScreen
import com.sabeel.steps.ui.screens.AboutScreen
import com.sabeel.steps.ui.screens.HistoryScreen
import com.sabeel.steps.ui.screens.HomeScreen
import com.sabeel.steps.ui.screens.InsightsScreen
import com.sabeel.steps.ui.screens.OnboardingScreen
import com.sabeel.steps.ui.screens.PrivacyScreen
import com.sabeel.steps.ui.screens.SettingsScreen
import com.sabeel.steps.ui.screens.WalkScreen
import com.sabeel.steps.ui.viewmodel.AppViewModel
import kotlinx.coroutines.launch

/** Single source of truth for navigation routes. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val WALK = "walk"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}

private data class TabItem(val route: String, val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TabItem(Routes.HISTORY, R.string.nav_history, Icons.Filled.DateRange),
    TabItem(Routes.INSIGHTS, R.string.nav_insights, Icons.Filled.Star),
    TabItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

/**
 * Root of the UI: one NavHost, one scaffold that consumes the window insets, and a bottom bar that
 * only exists on the four main destinations (Walk, Privacy and About are pushed flows).
 */
@Composable
fun SabeelRoot(container: AppContainer) {
    val context = LocalContext.current
    val app = context.applicationContext as SabeelApp
    val appViewModel: AppViewModel = viewModel(factory = AppViewModel.factory(app))
    val appState by appViewModel.state.collectAsStateWithLifecycle()
    if (appState.loading) {
        LoadingScreen()
        return
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (appState.onboardingDone) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(onStartWalk = { navController.navigate(Routes.WALK) })
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.INSIGHTS) { InsightsScreen() }
            composable(Routes.WALK) {
                WalkScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onDeletedAll = {
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onMessage = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                )
            }
        }
    }
}
