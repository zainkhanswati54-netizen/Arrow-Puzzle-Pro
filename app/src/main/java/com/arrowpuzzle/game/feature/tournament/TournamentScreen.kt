package com.arrowpuzzle.game.feature.tournament

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.gentleFloat
import com.arrowpuzzle.game.core.motion.pulse
import com.arrowpuzzle.game.core.ui.PrimaryPillButton
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/**
 * A single row on the leaderboard.
 *
 * [isPlayer] marks the row that belongs to whoever is looking at the screen, so it
 * can be highlighted and — on a real backend — kept in view via auto-scroll.
 */
data class TournamentEntry(
    val rank: Int,
    val name: String,
    val points: Int,
    val isPlayer: Boolean = false
)

/**
 * Deterministic mock leaderboard so the screen has believable content without a
 * backend yet. Swap [generateLeaderboard] for a real repository call later — the
 * screen itself only cares about the [TournamentEntry] list, so nothing else changes.
 */
private val adjectives = listOf(
    "Embarrassed", "Hungry", "Colorful", "Witty", "Wild", "Modern", "Fair",
    "Faithful", "Alert", "Determined", "Comfortable", "Clever", "Brave",
    "Curious", "Gentle", "Swift", "Bold", "Quiet", "Playful", "Loyal"
)
private val animals = listOf(
    "Tarsier", "Okapi", "Kitten", "Nightingale", "Jellyfish", "Rook", "Chamois",
    "Reindeer", "Dragonfly", "Curlew", "Raccoon", "Anteater", "Otter", "Falcon",
    "Panther", "Heron", "Lynx", "Sparrow", "Badger", "Fox"
)

private fun generateLeaderboard(playerPoints: Int, playerRank: Int, size: Int = 40): List<TournamentEntry> {
    val random = Random(20260811) // stable seed: same board on every open, this week
    val used = mutableSetOf<String>()
    val entries = mutableListOf<TournamentEntry>()
    var lastPoints = playerPoints + playerRank * 80

    repeat(size) { i ->
        var name: String
        do {
            name = "${adjectives.random(random)} ${animals.random(random)}"
        } while (!used.add(name))

        val points = (lastPoints - random.nextInt(20, 220)).coerceAtLeast(4)
        lastPoints = points
        entries += TournamentEntry(rank = i + 1, name = name, points = points)
    }

    val insertIndex = (playerRank - 1).coerceIn(0, entries.lastIndex)
    val withPlayer = entries.toMutableList()
    withPlayer[insertIndex] = TournamentEntry(
        rank = insertIndex + 1,
        name = "You",
        points = playerPoints,
        isPlayer = true
    )
    // Re-number ranks 1..size in order, keeping the sort by points descending.
    return withPlayer
        .sortedByDescending { it.points }
        .mapIndexed { index, e -> e.copy(rank = index + 1) }
}

/** Time remaining until the tournament resets — every Monday at midnight, local time. */
private fun timeUntilReset(): Duration {
    val now = LocalDateTime.now()
    val nextMonday = now.toLocalDate()
        .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        .atTime(LocalTime.MIDNIGHT)
    return Duration.between(now, nextMonday)
}

private fun formatCountdown(d: Duration): String {
    val days = d.toDays()
    val hours = d.toHours() % 24
    return "${days}d ${hours}h"
}

@Composable
fun TournamentScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    /** Points the signed-in player has earned so far this tournament week. */
    playerPoints: Int = 640,
    /** Where the player currently sits before the mock board is merged in. */
    playerRank: Int = 24
) {
    var showInfo by remember { mutableStateOf(false) }
    val leaderboard = remember(playerPoints, playerRank) {
        generateLeaderboard(playerPoints, playerRank)
    }
    val countdownText = remember { formatCountdown(timeUntilReset()) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TournamentHeader(
                countdownText = countdownText,
                onBack = onBack,
                onInfo = { showInfo = true }
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                color = AppTheme.palette.canvas,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 18.dp,
                        bottom = 110.dp,
                        start = 4.dp,
                        end = 4.dp
                    )
                ) {
                    items(leaderboard, key = { it.rank }) { entry ->
                        LeaderboardRow(
                            entry = entry,
                            modifier = Modifier.enterFromBelow(
                                delayMillis = Motion.stagger((entry.rank - 1).coerceAtMost(10)),
                                key = entry.rank
                            )
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            PrimaryPillButton(
                text = "Play",
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
                brush = AppTheme.palette.tournamentCardBrush
            )
        }
    }

    TournamentInfoDialog(visible = showInfo, onDismiss = { showInfo = false })
}

@Composable
private fun TournamentHeader(
    countdownText: String,
    onBack: () -> Unit,
    onInfo: () -> Unit
) {
    val palette = AppTheme.palette

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.tournamentCardBrush)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(56.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderIconButton(icon = Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", onClick = onBack)
                Text(
                    text = "Tournament",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                HeaderIconButton(icon = Icons.Rounded.Info, contentDescription = "How it works", onClick = onInfo)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(14.dp))

            MedalTrio(modifier = Modifier.align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Bronze, gold (larger, foregrounded), silver — same shape language as the reference. */
@Composable
private fun MedalTrio(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy((-18).dp)
    ) {
        Medal(
            place = 3,
            size = 72.dp,
            colors = listOf(Color(0xFFE8A15C), Color(0xFFB9702E)),
            modifier = Modifier.gentleFloat(amplitudeDp = 3f, phase = 0.1f)
        )
        Medal(
            place = 1,
            size = 96.dp,
            colors = listOf(Color(0xFFFFE28A), Color(0xFFF0A93C)),
            modifier = Modifier
                .zIndexCompat()
                .pulse(min = 0.98f, max = 1.03f)
        )
        Medal(
            place = 2,
            size = 72.dp,
            colors = listOf(Color(0xFFE7ECF3), Color(0xFFB9C2CF)),
            modifier = Modifier.gentleFloat(amplitudeDp = 3f, phase = 0.6f)
        )
    }
}

/** No elevation API needed here — later medals in the Row simply paint on top. */
private fun Modifier.zIndexCompat(): Modifier = this

@Composable
private fun Medal(place: Int, size: androidx.compose.ui.unit.Dp, colors: List<Color>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.EmojiEvents,
            contentDescription = "Rank $place",
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
private fun LeaderboardRow(entry: TournamentEntry, modifier: Modifier = Modifier) {
    val palette = AppTheme.palette
    val rowColor = if (entry.isPlayer) Blue500.copy(alpha = 0.08f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(rowColor)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.rank}",
            style = MaterialTheme.typography.titleMedium,
            color = if (entry.isPlayer) Blue500 else palette.inkMuted,
            modifier = Modifier.width(34.dp)
        )
        Text(
            text = entry.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (entry.isPlayer) FontWeight.Bold else FontWeight.Normal,
            color = palette.ink,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(palette.canvasSunken)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.NorthEast,
                contentDescription = null,
                tint = palette.inkSoft,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "%,d".format(entry.points),
                style = MaterialTheme.typography.labelLarge,
                color = palette.ink
            )
        }
    }
}

private data class TournamentInfoPage(
    val icon: ImageVector,
    val iconBackground: Brush,
    val iconTint: Color,
    val body: String
)

/** Three-page "How Does It Work" explainer, same pattern as the daily-challenge intro. */
@Composable
private fun TournamentInfoDialog(visible: Boolean, onDismiss: () -> Unit) {
    if (!visible) return

    val palette = AppTheme.palette
    var page by remember { mutableIntStateOf(0) }

    val pages = remember {
        listOf(
            TournamentInfoPage(
                icon = Icons.Rounded.EmojiEvents,
                iconBackground = Brush.verticalGradient(listOf(Color(0xFFFFE28A), Color(0xFFF0A93C))),
                iconTint = Color.White,
                body = "Participate in the tournament and win a unique reward!"
            ),
            TournamentInfoPage(
                icon = Icons.Rounded.EmojiEvents,
                iconBackground = Brush.verticalGradient(listOf(Color(0xFFF4F7FC), Color(0xFFE9EFF8))),
                iconTint = Color(0xFFF5A623),
                body = "Complete levels and get as many points as possible!"
            ),
            TournamentInfoPage(
                icon = Icons.Rounded.Leaderboard,
                iconBackground = Brush.verticalGradient(listOf(Color(0xFFDCEBFE), Color(0xFF7FB6FA))),
                iconTint = Color(0xFF1462CE),
                body = "Climb the leaderboard before the timer runs out — the top 3 places take home medals."
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardState = remember { MutableTransitionState(false) }
            cardState.targetState = true

            AnimatedVisibility(
                visibleState = cardState,
                enter = fadeIn(tween(Motion.Normal)) +
                    scaleIn(initialScale = 0.9f, animationSpec = Motion.bouncy()),
                exit = fadeOut(tween(Motion.Quick)) + scaleOut(targetScale = 0.95f)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = palette.surface,
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "How Does It Work?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        androidx.compose.animation.AnimatedContent(
                            targetState = page,
                            transitionSpec = {
                                val forward = targetState > initialState
                                val width = if (forward) 1 else -1
                                (
                                    slideInHorizontally(
                                        tween(Motion.Normal, easing = Motion.Emphasized)
                                    ) { width * it / 3 } + fadeIn(tween(Motion.Normal))
                                    ) togetherWith (
                                    slideOutHorizontally(
                                        tween(Motion.Normal, easing = Motion.Exit)
                                    ) { -width * it / 3 } + fadeOut(tween(Motion.Quick))
                                    )
                            },
                            label = "tournamentInfoPage"
                        ) { index ->
                            val content = pages[index]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .pulse(min = 0.97f, max = 1.04f, periodMillis = 2800)
                                        .size(150.dp)
                                        .clip(CircleShape)
                                        .background(content.iconBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = content.icon,
                                        contentDescription = null,
                                        tint = content.iconTint,
                                        modifier = Modifier.size(76.dp)
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                Text(
                                    text = content.body,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.inkMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        InfoPageDots(count = pages.size, selected = page)

                        Spacer(Modifier.height(22.dp))

                        PrimaryPillButton(
                            text = if (page < pages.lastIndex) "Continue" else "Got it",
                            onClick = {
                                if (page < pages.lastIndex) page++ else onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val active = index == selected
            val width by animateDpAsState(
                targetValue = if (active) 20.dp else 8.dp,
                animationSpec = Motion.bouncy(),
                label = "dotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (active) Blue500 else AppTheme.palette.inkMuted.copy(alpha = 0.35f),
                animationSpec = Motion.smooth(Motion.Quick),
                label = "dotColor"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
