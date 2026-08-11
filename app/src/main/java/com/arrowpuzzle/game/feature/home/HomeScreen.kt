package com.arrowpuzzle.game.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.gentleFloat
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.ui.ArrowBackdrop
import com.arrowpuzzle.game.core.ui.PrimaryPillButton
import com.arrowpuzzle.game.core.ui.StatPill
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The screen that has to sell the game in one glance: two live mode cards at the
 * top, the wordmark holding the middle, and one unmissable action at the bottom.
 * Elements arrive in that order so the eye is led down to the button.
 */
@Composable
fun HomeScreen(
    onPlayCampaign: () -> Unit,
    onPlayDaily: () -> Unit,
    onOpenTournament: () -> Unit,
    modifier: Modifier = Modifier,
    currentLevel: Int = 1,
    tournamentRank: Int = 112,
    tournamentEndsIn: String = "8d 07h"
) {
    val palette = AppTheme.palette
    val spacing = AppTheme.spacing
    val todayLabel = remember {
        runCatching {
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
        }.getOrDefault("Today")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        ArrowBackdrop(tint = Color(0xFFE8EDF4))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                ModeCard(
                    modifier = Modifier
                        .weight(1f)
                        .enterFromBelow(delayMillis = Motion.stagger(0)),
                    brush = palette.dailyCardBrush,
                    eyebrow = "Daily challenge",
                    title = todayLabel,
                    icon = Icons.Rounded.Event,
                    onPlay = onPlayDaily
                )
                ModeCard(
                    modifier = Modifier
                        .weight(1f)
                        .enterFromBelow(delayMillis = Motion.stagger(1)),
                    brush = palette.tournamentCardBrush,
                    eyebrow = "Tournament",
                    title = "#$tournamentRank",
                    icon = Icons.Rounded.EmojiEvents,
                    topEndSlot = {
                        StatPill(
                            text = tournamentEndsIn,
                            leading = {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        )
                    },
                    onPlay = onOpenTournament
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Arrow Puzzle Pro",
                style = MaterialTheme.typography.displayLarge,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .enterFromBelow(delayMillis = Motion.stagger(2), travel = 34f)
                    .gentleFloat(amplitudeDp = 3.5f, periodMillis = 5200)
            )

            Spacer(Modifier.weight(1.2f))

            PrimaryPillButton(
                text = "New Game",
                subtitle = "Level $currentLevel",
                onClick = onPlayCampaign,
                modifier = Modifier
                    .enterFromBelow(delayMillis = Motion.stagger(3))
                    .fillMaxWidth()
            )

            Spacer(Modifier.height(spacing.xxl))
        }
    }
}

/** One of the two coloured tiles at the top of the menu. */
@Composable
private fun ModeCard(
    brush: Brush,
    eyebrow: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    topEndSlot: (@Composable () -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .aspectRatio(0.92f)
            .pressScale(interactionSource, pressedScale = 0.96f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF1F2A3D).copy(alpha = 0.16f),
                spotColor = Color(0xFF1F2A3D).copy(alpha = 0.20f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onPlay
            )
    ) {
        if (topEndSlot != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) { topEndSlot() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .gentleFloat(amplitudeDp = 3f, periodMillis = 4200)
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = eyebrow.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (title.startsWith("#")) {
                    Icon(
                        imageVector = Icons.Rounded.NorthEast,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 2.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.26f))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onPlay
                    )
                    .padding(horizontal = 22.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}
