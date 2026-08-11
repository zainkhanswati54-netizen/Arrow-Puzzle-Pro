package com.arrowpuzzle.game.feature.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arrowpuzzle.game.core.data.AppSettings
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
import com.arrowpuzzle.game.feature.daily.DailyViewModel
import com.arrowpuzzle.game.feature.game.GameViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared scaffold every info screen in this file sits inside. */
@Composable
private fun InfoScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val palette = AppTheme.palette
    val spacing = AppTheme.spacing
    Column(modifier.fillMaxSize().background(palette.canvasSunken)) {
        AppTopBar(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Spacer(Modifier.height(spacing.sm))
            content()
            Spacer(Modifier.height(spacing.huge))
        }
    }
}

@Composable
private fun ProseCard(title: String, body: String, index: Int) {
    GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(index))) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = AppTheme.palette.ink, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = AppTheme.palette.inkMuted)
        }
    }
}

// ── Help ─────────────────────────────────────────────────────────────────────

private data class Faq(val question: String, val answer: String)

private val FAQS = listOf(
    Faq("How do I clear an arrow?", "Tap any arrow whose path to the edge of the board is clear. It slides off and the tiles behind it open up."),
    Faq("What happens if I tap a blocked arrow?", "A blocked tap costs one of your three lives. Lose all three and the level resets."),
    Faq("What does the hint button do?", "It highlights one arrow that's free to clear right now. You get three hints per level."),
    Faq("How does the daily challenge work?", "One new puzzle unlocks every 24 hours. Clear it to star that day on the calendar — the button re-enables the next day."),
    Faq("Is my progress saved?", "Yes. Campaign progress and daily stars are saved on this device automatically as you play.")
)

@Composable
fun HelpScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScaffold(title = "Help", onBack = onBack, modifier = modifier) {
        Text(
            "Everything you need to know about clearing the board.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.palette.inkMuted
        )
        Spacer(Modifier.height(4.dp))
        FAQS.forEachIndexed { i, faq -> ProseCard(faq.question, faq.answer, i) }
    }
}

// ── About ────────────────────────────────────────────────────────────────────

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScaffold(title = "About Game", onBack = onBack, modifier = modifier) {
        GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(0))) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Extension, contentDescription = null, tint = Blue500, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text("Arrow Puzzle Pro", style = MaterialTheme.typography.headlineSmall, color = AppTheme.palette.ink)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = AppTheme.palette.inkMuted)
            }
        }
        ProseCard(
            "How to play",
            "Tap arrows to clear them off the board. An arrow can only leave if nothing blocks its path to the edge — plan ahead before you tap.",
            1
        )
        ProseCard(
            "Credits",
            "Designed and built with Jetpack Compose. Thanks for playing.",
            2
        )
    }
}

// ── Privacy Rights ───────────────────────────────────────────────────────────

@Composable
fun PrivacyRightsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    InfoScaffold(title = "Privacy Rights", onBack = onBack, modifier = modifier) {
        ProseCard(
            "Your data, your call",
            "Arrow Puzzle Pro stores your game progress locally on this device. You can request a copy of it or delete it at any time.",
            0
        )
        ProseCard(
            "Delete your data",
            "Uninstalling the app permanently removes all locally stored progress, settings and daily-challenge history from this device.",
            1
        )
        ProseCard(
            "Access your data",
            "Since progress is stored only on-device, you already have full access to it — nothing is uploaded without your consent in Privacy Preferences.",
            2
        )
    }
}

// ── Privacy Preferences ──────────────────────────────────────────────────────

@Composable
fun PrivacyPreferencesScreen(
    settings: AppSettings,
    onAnalytics: (Boolean) -> Unit,
    onPersonalizedAds: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoScaffold(title = "Privacy Preferences", onBack = onBack, modifier = modifier) {
        Text(
            "Choose what you share. These apply on this device only.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.palette.inkMuted
        )
        GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(0))) {
            PreferenceToggleRow(
                label = "Analytics",
                description = "Share anonymous gameplay stats to help improve the game.",
                checked = settings.analyticsEnabled,
                onCheckedChange = onAnalytics,
                showDivider = true
            )
            PreferenceToggleRow(
                label = "Personalized ads",
                description = "Use your activity to show more relevant ads.",
                checked = settings.personalizedAdsEnabled,
                onCheckedChange = onPersonalizedAds
            )
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    val palette = AppTheme.palette
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                Text(description, style = MaterialTheme.typography.bodySmall, color = palette.inkMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Blue500)
            )
        }
        if (showDivider) {
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.HorizontalDivider(color = palette.inkMuted.copy(alpha = 0.12f))
        }
    }
}

// ── Achievements ─────────────────────────────────────────────────────────────

private data class Achievement(val title: String, val body: String, val unlocked: Boolean)

@Composable
fun AchievementsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val level by GameViewModel.readProgress(context).collectAsState(initial = 1)
    val dailyVm: DailyViewModel = viewModel(factory = DailyViewModel.factory(context))
    val dailyState by dailyVm.state.collectAsState()
    val totalStars = dailyState.completedDays.size

    val achievements = remember(level, totalStars) {
        listOf(
            Achievement("First Steps", "Clear your first level.", level > 1),
            Achievement("Getting Sharp", "Reach level 10.", level >= 10),
            Achievement("Puzzle Master", "Reach level 30.", level >= 30),
            Achievement("First Star", "Complete one daily challenge.", totalStars >= 1),
            Achievement("Weekly Streak", "Earn 7 daily stars.", totalStars >= 7)
        )
    }

    InfoScaffold(title = "Achievements", onBack = onBack, modifier = modifier) {
        achievements.forEachIndexed { i, a ->
            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(i))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background((if (a.unlocked) Lime500 else AppTheme.palette.inkMuted).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (a.unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = if (a.unlocked) Lime500 else AppTheme.palette.inkMuted
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium, color = AppTheme.palette.ink)
                        Text(a.body, style = MaterialTheme.typography.bodySmall, color = AppTheme.palette.inkMuted)
                    }
                }
            }
        }
    }
}

// ── Awards ───────────────────────────────────────────────────────────────────

@Composable
fun AwardsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dailyVm: DailyViewModel = viewModel(factory = DailyViewModel.factory(context))
    val dailyState by dailyVm.state.collectAsState()

    val months = remember(dailyState) {
        val today = YearMonth.now()
        (0..5).map { offset -> today.minusMonths(offset.toLong()) }
    }

    InfoScaffold(title = "Awards", onBack = onBack, modifier = modifier) {
        Text(
            "Finish every daily challenge in a month to earn that month's trophy.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.palette.inkMuted
        )
        months.forEachIndexed { i, month ->
            val stars = dailyState.starsIn(month)
            val complete = stars >= month.lengthOfMonth() && month.isBefore(YearMonth.now().plusMonths(1))
            GroupCard(modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(i))) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background((if (complete) Amber500 else AppTheme.palette.inkMuted).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (complete) Icons.Rounded.EmojiEvents else Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = if (complete) Amber500 else AppTheme.palette.inkMuted
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.palette.ink
                        )
                        Text(
                            "$stars/${month.lengthOfMonth()} days completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.palette.inkMuted
                        )
                    }
                }
            }
        }
    }
}
