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
import com.arrowpuzzle.game.core.design.Orange600
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.ui.ComingSoonScreen
import com.arrowpuzzle.game.core.ui.SoftFade
import com.arrowpuzzle.game.feature.consent.ConsentScreen
import com.arrowpuzzle.game.feature.daily.DailyScreen
import com.arrowpuzzle.game.feature.game.GameScreen
import com.arrowpuzzle.game.feature.game.GameViewModel
import com.arrowpuzzle.game.feature.home.HomeScreen
import com.arrowpuzzle.game.feature.info.AboutScreen
import com.arrowpuzzle.game.feature.info.AchievementsScreen
import com.arrowpuzzle.game.feature.info.AwardsScreen
import com.arrowpuzzle.game.feature.info.HelpScreen
import com.arrowpuzzle.game.feature.info.PrivacyPreferencesScreen
import com.arrowpuzzle.game.feature.info.PrivacyRightsScreen
import com.arrowpuzzle.game.feature.me.MeScreen
import com.arrowpuzzle.game.feature.settings.SettingsScreen
import com.arrowpuzzle.game.feature.splash.SplashScreen
import com.arrowpuzzle.game.feature.tournament.TournamentScreen

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
                        onPlayDaily = { navController.navigate(Routes.DailyGame) },
                        onOpenTournament = { navController.navigate(Routes.Tournament) }
                    )
                }

                composable(Routes.Daily) {
                    DailyScreen(
                        introSeen = settings.dailyIntroSeen,
                        onIntroDismissed = appViewModel::markDailyIntroSeen,
                        onPlay = { navController.navigate(Routes.DailyGame) },
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

                composable(Routes.DailyGame) {
                    val dailyVm: com.arrowpuzzle.game.feature.daily.DailyViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = com.arrowpuzzle.game.feature.daily.DailyViewModel.factory(ctx)
                    )
                    GameScreen(
                        levelId = dailyVm.todaysLevelId(),
                        isDaily = true,
                        onDailyComplete = {
                            dailyVm.onChallengeCompleted()
                            navController.popBackStack()
                        },
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
                    AwardsScreen(onBack = navController::popBackStack)
                }

                composable(Routes.Achievements) {
                    AchievementsScreen(onBack = navController::popBackStack)
                }

                composable(Routes.Help) {
                    HelpScreen(onBack = navController::popBackStack)
                }

                composable(Routes.About) {
                    AboutScreen(onBack = navController::popBackStack)
                }

                composable(Routes.PrivacyRights) {
                    PrivacyRightsScreen(onBack = navController::popBackStack)
                }

                composable(Routes.PrivacyPreferences) {
                    PrivacyPreferencesScreen(
                        settings = settings,
                        onAnalytics = appViewModel::setAnalytics,
                        onPersonalizedAds = appViewModel::setPersonalizedAds,
                        onBack = navController::popBackStack
                    )
                }

                composable(Routes.Tournament) {
                    TournamentScreen(
                        onBack = navController::popBackStack,
                        onPlay = { navController.navigate(Routes.game(mode = GameMode.Tournament, levelId = savedLevel)) }
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
