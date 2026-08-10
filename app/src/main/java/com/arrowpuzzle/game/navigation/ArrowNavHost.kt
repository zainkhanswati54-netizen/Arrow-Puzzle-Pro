package com.arrowpuzzle.game.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arrowpuzzle.game.core.data.AppViewModel
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Amber500
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Green500
import com.arrowpuzzle.game.core.design.Indigo500
import com.arrowpuzzle.game.core.design.Lime500
import com.arrowpuzzle.game.core.design.Orange600
import com.arrowpuzzle.game.core.design.Red500
import com.arrowpuzzle.game.core.design.Teal500
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.ui.ComingSoonScreen
import com.arrowpuzzle.game.core.ui.SoftFade
import com.arrowpuzzle.game.feature.consent.ConsentScreen
import com.arrowpuzzle.game.feature.daily.DailyScreen
import com.arrowpuzzle.game.feature.game.GameScreen
import com.arrowpuzzle.game.feature.game.GameViewModel
import com.arrowpuzzle.game.feature.home.HomeScreen
import com.arrowpuzzle.game.feature.me.MeScreen
import com.arrowpuzzle.game.feature.settings.SettingsScreen
import com.arrowpuzzle.game.feature.splash.SplashScreen

private const val SlideFraction = 6

/** Detail pushes come in from the right; tab swaps only crossfade. */
private fun isTabSwap(scope: AnimatedContentTransitionScope<NavBackStackEntry>): Boolean {
    val from = scope.initialState.destination.route ?: return false
    val to = scope.targetState.destination.route ?: return false
    return from in TabRoutes && to in TabRoutes
}

private fun enter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition =
    if (isTabSwap(scope)) {
        fadeIn(tween(Motion.Normal, easing = Motion.Emphasized)) +
            scaleIn(initialScale = 0.985f, animationSpec = tween(Motion.Normal, easing = Motion.Emphasized))
    } else {
        slideInHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { it / SlideFraction } +
            fadeIn(tween(Motion.Normal, easing = Motion.Emphasized))
    }

private fun exit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition =
    if (isTabSwap(scope)) {
        fadeOut(tween(Motion.Quick, easing = Motion.Exit)) +
            scaleOut(targetScale = 0.985f, animationSpec = tween(Motion.Quick, easing = Motion.Exit))
    } else {
        slideOutHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { -it / (SlideFraction * 2) } +
            fadeOut(tween(Motion.Quick, easing = Motion.Exit))
    }

private fun popEnter(scope: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition =
    if (isTabSwap(scope)) {
        enter(scope)
    } else {
        slideInHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { -it / (SlideFraction * 2) } +
            fadeIn(tween(Motion.Normal, easing = Motion.Emphasized))
    }

private fun popExit(scope: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition =
    if (isTabSwap(scope)) {
        exit(scope)
    } else {
        slideOutHorizontally(tween(Motion.Slow, easing = Motion.Emphasized)) { it / SlideFraction } +
            fadeOut(tween(Motion.Quick, easing = Motion.Exit))
    }

@Composable
fun ArrowPuzzleApp(
    appViewModel: AppViewModel,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val settings by appViewModel.settings.collectAsState()
    val ctx = LocalContext.current
    val savedLevel by GameViewModel.readProgress(ctx).collectAsState(initial = 1)

    SoundEngine.setEnabled(settings.soundEnabled)

    val switchTab: (String) -> Unit = { route ->
        if (route != currentRoute) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppTheme.palette.canvas)
    ) {
        Column(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Routes.Splash,
                modifier = Modifier.weight(1f),
                enterTransition = { enter(this) },
                exitTransition = { exit(this) },
                popEnterTransition = { popEnter(this) },
                popExitTransition = { popExit(this) }
            ) {
                composable(
                    route = Routes.Splash,
                    enterTransition = { fadeIn(tween(0)) },
                    exitTransition = { fadeOut(tween(Motion.Slow, easing = Motion.Exit)) }
                ) {
                    SplashScreen(
                        onReady = {
                            val next = if (settings.consentAccepted) Routes.Home else Routes.Consent
                            navController.navigate(next) {
                                popUpTo(Routes.Splash) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Routes.Consent,
                    enterTransition = { fadeIn(tween(Motion.Slow, easing = Motion.Emphasized)) }
                ) {
                    ConsentScreen(
                        onAccept = {
                            appViewModel.acceptConsent()
                            navController.navigate(Routes.Home) {
                                popUpTo(Routes.Consent) { inclusive = true }
                            }
                        },
                        onOpenOptions = { navController.navigate(Routes.PrivacyPreferences) }
                    )
                }

                // --- Tabs ---------------------------------------------------
                composable(Routes.Home) {
                    HomeScreen(
                        onPlayCampaign = { navController.navigate(Routes.game(levelId = savedLevel)) },
                        onPlayDaily = { navController.navigate(Routes.game(GameMode.Daily)) },
                        onOpenTournament = { navController.navigate(Routes.Tournament) }
                    )
                }

                composable(Routes.Daily) {
                    DailyScreen(
                        introSeen = settings.dailyIntroSeen,
                        onIntroDismissed = appViewModel::markDailyIntroSeen,
                        onPlay = { navController.navigate(Routes.game(GameMode.Daily)) },
                        onBack = { switchTab(Routes.Home) }
                    )
                }

                composable(Routes.Me) {
                    MeScreen(onNavigate = navController::navigate)
                }

                // --- Pushed detail screens ----------------------------------
                composable(Routes.Game) { entry ->
                    val levelId = entry.arguments?.getString("levelId")?.toIntOrNull() ?: 1
                    GameScreen(
                        levelId = levelId,
                        onExit = navController::popBackStack
                    )
                }

                composable(Routes.Settings) {
                    SettingsScreen(
                        settings = settings,
                        onSound = appViewModel::setSound,
                        onMusic = appViewModel::setMusic,
                        onHaptics = appViewModel::setHaptics,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.Awards) {
                    ComingSoonScreen(
                        title = "Awards",
                        headline = "Your trophy shelf",
                        body = "Finish every daily challenge in a month to earn that month's trophy. The shelf fills up here.",
                        icon = Icons.Rounded.EmojiEvents,
                        accent = Amber500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.Achievements) {
                    ComingSoonScreen(
                        title = "Achievements",
                        headline = "Milestones worth chasing",
                        body = "Perfect clears, no-hint runs, streaks. Each one unlocks a badge you can show off.",
                        icon = Icons.Rounded.SportsEsports,
                        accent = Lime500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.Help) {
                    ComingSoonScreen(
                        title = "Help",
                        headline = "How the arrows work",
                        body = "An interactive tutorial and answers to the questions players actually ask.",
                        icon = Icons.Rounded.SupportAgent,
                        accent = Green500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.About) {
                    ComingSoonScreen(
                        title = "About Game",
                        headline = "Arrow Puzzle Pro",
                        body = "Version 1.0.0 — Tap arrows to clear the maze. Credits, licences and release notes land here.",
                        icon = Icons.Rounded.Info,
                        accent = Blue500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.PrivacyRights) {
                    ComingSoonScreen(
                        title = "Privacy Rights",
                        headline = "Your data, your call",
                        body = "Request a copy of your data or delete it entirely. Wired up before the first release.",
                        icon = Icons.Rounded.VerifiedUser,
                        accent = Indigo500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.PrivacyPreferences) {
                    ComingSoonScreen(
                        title = "Privacy Preferences",
                        headline = "Choose what you share",
                        body = "Per-purpose consent toggles for analytics and personalised ads.",
                        icon = Icons.Rounded.PrivacyTip,
                        accent = Teal500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.RemoveAds) {
                    ComingSoonScreen(
                        title = "Remove Ads",
                        headline = "Play without interruptions",
                        body = "A one-time purchase that removes every ad. Store integration is next up.",
                        icon = Icons.Rounded.Payments,
                        accent = Red500,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.Tournament) {
                    ComingSoonScreen(
                        title = "Tournament",
                        headline = "Compete on a fresh board",
                        body = "Weekly brackets with a shared puzzle set and a live leaderboard.",
                        icon = Icons.Rounded.EmojiEvents,
                        accent = Orange600,
                        onBack = navController::popBackStack
                    )
                }
            }

            SoftFade(visible = currentRoute != null && currentRoute in TabRoutes) {
                ArrowBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = switchTab
                )
            }
        }
    }
}
