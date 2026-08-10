package com.arrowpuzzle.game.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.data.AppSettings
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Amber500
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Indigo500
import com.arrowpuzzle.game.core.design.Red500
import com.arrowpuzzle.game.core.design.Teal500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.ui.AppTopBar
import com.arrowpuzzle.game.core.ui.ComingSoonBadge
import com.arrowpuzzle.game.core.ui.GroupCard
import com.arrowpuzzle.game.core.ui.SettingsRow

/**
 * The only screen in this build where controls actually change state, so the
 * toggles are worth getting right: thumb travel is a spring, the track colour is
 * a tween, and both are driven from persisted state rather than local memory.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSound: (Boolean) -> Unit,
    onMusic: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val spacing = AppTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasSunken)
    ) {
        AppTopBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Spacer(Modifier.height(spacing.sm))

            SectionLabel("Audio & feel")

            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(0))) {
                ToggleRow(
                    label = "Sound effects",
                    icon = Icons.Rounded.VolumeUp,
                    tint = Blue500,
                    checked = settings.soundEnabled,
                    onCheckedChange = onSound,
                    showDivider = true
                )
                ToggleRow(
                    label = "Music",
                    icon = Icons.Rounded.LibraryMusic,
                    tint = Indigo500,
                    checked = settings.musicEnabled,
                    onCheckedChange = onMusic,
                    showDivider = true
                )
                ToggleRow(
                    label = "Haptics",
                    icon = Icons.Rounded.Vibration,
                    tint = Teal500,
                    checked = settings.hapticsEnabled,
                    onCheckedChange = onHaptics
                )
            }

            Spacer(Modifier.height(spacing.xs))

            SectionLabel("Game")

            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(1))) {
                SettingsRow(
                    label = "Language",
                    icon = Icons.Rounded.Language,
                    tint = Amber500,
                    showDivider = true,
                    trailing = { ComingSoonBadge() },
                    onClick = {}
                )
                SettingsRow(
                    label = "Reset progress",
                    icon = Icons.Rounded.RestartAlt,
                    tint = Red500,
                    trailing = { ComingSoonBadge() },
                    onClick = {}
                )
            }

            Spacer(Modifier.height(spacing.huge))
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AppTheme.palette.inkMuted,
        modifier = modifier.padding(start = 6.dp, top = 6.dp)
    )
}

@Composable
private fun ToggleRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    SettingsRow(
        label = label,
        icon = icon,
        tint = tint,
        showChevron = false,
        showDivider = showDivider,
        modifier = modifier,
        trailing = { SpringSwitch(checked = checked) },
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheckedChange(!checked)
        }
    )
}

/** A switch with a thumb that overshoots slightly — the standard one does not. */
@Composable
private fun SpringSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = Motion.bouncy(),
        label = "thumbOffset"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) Blue500 else palette.inkMuted.copy(alpha = 0.30f),
        animationSpec = Motion.smooth(Motion.Quick),
        label = "trackColor"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { role = Role.Switch },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = thumbOffset)
                .shadow(4.dp, CircleShape)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
