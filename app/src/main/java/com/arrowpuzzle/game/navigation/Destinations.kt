package com.arrowpuzzle.game.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Routes are declared once, here. Nothing else in the app types a raw string —
 * that is what keeps deep links and the bottom bar from drifting apart later.
 */
object Routes {
    const val Splash = "splash"
    const val Consent = "consent"

    const val Home = "home"
    const val Daily = "daily"
    const val Me = "me"

    const val Game = "game/{levelId}"
    fun game(mode: String = GameMode.Campaign, levelId: Int = 1) = "game/$levelId"

    const val Settings = "settings"
    const val Awards = "awards"
    const val Achievements = "achievements"
    const val Help = "help"
    const val About = "about"
    const val PrivacyRights = "privacy_rights"
    const val PrivacyPreferences = "privacy_preferences"
    const val RemoveAds = "remove_ads"
    const val Tournament = "tournament"
}

object GameMode {
    const val Campaign = "campaign"
    const val Daily = "daily"
    const val Tournament = "tournament"
}

/** The three destinations that keep the bottom bar visible. */
@Immutable
data class TabDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val BottomTabs: List<TabDestination> = listOf(
    TabDestination(Routes.Home, "Main", Icons.Rounded.Home, Icons.Outlined.Home),
    TabDestination(Routes.Daily, "Daily", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth),
    TabDestination(Routes.Me, "Me", Icons.Rounded.Person, Icons.Outlined.Person)
)

val TabRoutes: Set<String> = BottomTabs.map { it.route }.toSet()
