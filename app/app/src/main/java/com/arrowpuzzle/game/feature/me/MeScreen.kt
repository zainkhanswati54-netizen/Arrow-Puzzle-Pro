package com.arrowpuzzle.game.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Amber500
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Green500
import com.arrowpuzzle.game.core.design.Indigo500
import com.arrowpuzzle.game.core.design.Lime500
import com.arrowpuzzle.game.core.design.Teal500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.ui.AppTopBar
import com.arrowpuzzle.game.core.ui.GroupCard
import com.arrowpuzzle.game.core.ui.SettingsRow
import com.arrowpuzzle.game.navigation.Routes

/**
 * Profile hub. Grouped exactly as the reference: two hero rows, settings on its
 * own, the four information rows together, then the purchase row isolated at the
 * bottom so it never gets tapped by accident.
 */
@Composable
fun MeScreen(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val spacing = AppTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasSunken)
    ) {
        AppTopBar(title = "Me")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Spacer(Modifier.height(spacing.sm))

            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(0))) {
                SettingsRow(
                    label = "Awards",
                    icon = Icons.Rounded.EmojiEvents,
                    tint = Amber500,
                    emphasized = true,
                    onClick = { onNavigate(Routes.Awards) }
                )
            }

            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(1))) {
                SettingsRow(
                    label = "Achievements",
                    icon = Icons.Rounded.SportsEsports,
                    tint = Lime500,
                    emphasized = true,
                    showChevron = false,
                    onClick = { onNavigate(Routes.Achievements) }
                )
            }

            Spacer(Modifier.height(spacing.xs))

            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(2))) {
                SettingsRow(
                    label = "Settings",
                    icon = Icons.Rounded.Settings,
                    tint = Blue500,
                    onClick = { onNavigate(Routes.Settings) }
                )
            }

            Spacer(Modifier.height(spacing.xs))

            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(3))) {
                SettingsRow(
                    label = "Help",
                    icon = Icons.Rounded.SupportAgent,
                    tint = Green500,
                    showDivider = true,
                    onClick = { onNavigate(Routes.Help) }
                )
                SettingsRow(
                    label = "About Game",
                    icon = Icons.Rounded.Info,
                    tint = Blue500,
                    showDivider = true,
                    onClick = { onNavigate(Routes.About) }
                )
                SettingsRow(
                    label = "Privacy Rights",
                    icon = Icons.Rounded.VerifiedUser,
                    tint = Indigo500,
                    showDivider = true,
                    onClick = { onNavigate(Routes.PrivacyRights) }
                )
                SettingsRow(
                    label = "Privacy Preferences",
                    icon = Icons.Rounded.PrivacyTip,
                    tint = Teal500,
                    onClick = { onNavigate(Routes.PrivacyPreferences) }
                )
            }

            Spacer(Modifier.height(spacing.huge))
        }
    }
}
