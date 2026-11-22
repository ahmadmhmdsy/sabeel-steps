package com.sabeel.steps.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sabeel.steps.AppContainer
import com.sabeel.steps.R
import com.sabeel.steps.SabeelApp
import com.sabeel.steps.platform.CountingStore
import com.sabeel.steps.platform.Notifications
import com.sabeel.steps.ui.components.LoadingScreen
import com.sabeel.steps.ui.i18n.AppLocale
import com.sabeel.steps.ui.screens.AboutScreen
import com.sabeel.steps.ui.screens.DiagnosticsScreen
import com.sabeel.steps.ui.screens.HistoryScreen
import com.sabeel.steps.ui.screens.HomeScreen
import com.sabeel.steps.ui.screens.InsightsScreen
import com.sabeel.steps.ui.screens.OnboardingScreen
import com.sabeel.steps.ui.screens.PrivacyScreen
import com.sabeel.steps.ui.screens.SessionRoutes
import com.sabeel.steps.ui.screens.SessionSetupScreen
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
    const val DIAGNOSTICS = "diagnostics"
}

private data class TabItem(val route: String, val labelRes: Int, val iconRes: Int)

/** Authored line icons: dome, journal, sun, sliders (see the Q2-b prototypes). */
private val tabs = listOf(
    TabItem(Routes.HOME, R.string.nav_home, R.drawable.ic_nav_home),
    TabItem(Routes.HISTORY, R.string.nav_history, R.drawable.ic_nav_history),
    TabItem(Routes.INSIGHTS, R.string.nav_insights, R.drawable.ic_nav_insights),
    TabItem(Routes.SETTINGS, R.string.nav_settings, R.drawable.ic_nav_settings),
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

    // App-open re-arm: an app cannot keep starting background services while it is closed, so every
    // foreground entry re-checks the counter. ensureStarted() is safe without the permission (it
    // returns false and the Home status card explains why) and never wakes a service that is running.
    LifecycleResumeEffect(appState.onboardingDone) {
        if (appState.onboardingDone) {
            // Reconcile the persisted intent with the service: start when the user left counting on,
            // stop when they left it off. The controller re-checks the flag after every action, so a
            // rapid off/on cannot leave a zombie service behind.
            container.countingController.syncOnAppOpen()
            Notifications.ensureChannels(AppLocale.wrap(context))
        }
        onPauseOrDispose { }
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val countingState by container.counting.state.collectAsStateWithLifecycle()
    val countingOffSnack = stringResource(R.string.counting_off_snack)
    val countingOnSnack = stringResource(R.string.counting_on_snack)
    val undoLabel = stringResource(R.string.undo)
    // The ring/pill/Settings switch all dispatch this: persist the new intent first, then offer a
    // 5 s Undo. Undo only wins when the user has not toggled again meanwhile (the controller is the
    // source of truth, not the value captured when the snackbar appeared).
    val toggleCountingWithUndo: () -> Unit = {
        val previous = container.counting.current().enabled
        container.countingController.setEnabled(!previous, CountingStore.REASON_USER)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = if (previous) countingOffSnack else countingOnSnack,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed &&
                container.counting.current().enabled != previous
            ) {
                container.countingController.setEnabled(previous, CountingStore.REASON_USER)
            }
        }
    }
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
                                    painter = painterResource(tab.iconRes),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
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
                        // Onboarding completion is the one explicit moment the default-on intent
                        // becomes an explicit on intent; the controller persists it before acting.
                        container.countingController.markOnboardingDone()
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onStartWalk = { navController.navigate(SessionRoutes.SETUP) },
                    onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                    countingEnabled = countingState.enabled,
                    onToggleCounting = toggleCountingWithUndo,
                )
            }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.INSIGHTS) { InsightsScreen() }
            composable(SessionRoutes.SETUP) {
                SessionSetupScreen(
                    onBack = { navController.popBackStack() },
                    onSessionStarted = {
                        navController.navigate(SessionRoutes.LIVE) {
                            popUpTo(SessionRoutes.SETUP) { inclusive = true }
                        }
                    },
                )
            }
            composable(SessionRoutes.LIVE) {
                WalkScreen(
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                    onStartSetup = { navController.navigate(SessionRoutes.SETUP) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                    onDeletedAll = {
                        // Delete-all is a fresh start: the counting intent is reset too, and
                        // onboarding re-enables it explicitly on completion.
                        container.countingController.setEnabled(false, CountingStore.REASON_USER)
                        navController.navigate(Routes.ONBOARDING) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onMessage = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    countingEnabled = countingState.enabled,
                    onToggleCounting = toggleCountingWithUndo,
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
            composable(Routes.DIAGNOSTICS) {
                DiagnosticsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}