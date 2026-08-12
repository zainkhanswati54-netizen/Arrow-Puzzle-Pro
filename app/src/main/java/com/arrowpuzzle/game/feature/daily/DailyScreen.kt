package com.arrowpuzzle.game.feature.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.gentleFloat
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.motion.pulse
import com.arrowpuzzle.game.core.ui.ArrowBackdropScrim
import com.arrowpuzzle.game.core.ui.PrimaryPillButton
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Daily challenges. The month grid is the point of the screen, so it gets the
 * light surface and the header carries the reward fantasy above it.
 *
 * Grid cells animate in on a diagonal stagger rather than row-by-row — it takes
 * the same total time but reads as one gesture instead of seven.
 *
 * Exactly one challenge unlocks every 24 hours: completing it stars today's
 * cell for good, and Play stays disabled until the calendar rolls to a new
 * day, matching the "one level a day" loop.
 */
@Composable
fun DailyScreen(
    introSeen: Boolean,
    onIntroDismissed: () -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now()
) {
    val palette = AppTheme.palette
    var showIntro by remember(introSeen) { mutableStateOf(!introSeen) }

    val context = LocalContext.current
    val dailyVm: DailyViewModel = viewModel(factory = DailyViewModel.factory(context))
    val dailyState by dailyVm.state.collectAsState()

    val month = remember(today) { YearMonth.from(today) }
    val monthLabel = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    }
    val starsEarned = remember(dailyState, month) { dailyState.starsIn(month) }
    val playedToday = dailyState.playedToday

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surface)
    ) {
        Column(Modifier.fillMaxSize()) {

            DailyHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .enterFromBelow(delayMillis = Motion.stagger(0)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = palette.accentGold,
                        modifier = Modifier
                            .pulse(min = 0.94f, max = 1.06f, periodMillis = 3000)
                            .size(22.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "$starsEarned/${month.lengthOfMonth()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.ink
                    )
                }

                Spacer(Modifier.height(18.dp))

                MonthGrid(month = month, today = today, completedDays = dailyState.completedDays)

                Spacer(Modifier.weight(1f))

                PrimaryPillButton(
                    text = if (playedToday) "Come back tomorrow" else "Play",
                    onClick = onPlay,
                    enabled = !playedToday,
                    modifier = Modifier
                        .enterFromBelow(delayMillis = Motion.stagger(6))
                        .fillMaxWidth()
                )

                com.arrowpuzzle.game.core.ads.BannerAdView()

                Spacer(Modifier.height(24.dp))
            }
        }

        DailyIntroDialog(
            visible = showIntro,
            onDismiss = {
                showIntro = false
                onIntroDismissed()
            }
        )
    }
}

@Composable
private fun DailyHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val palette = AppTheme.palette
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(palette.dailyHeaderBrush)
    ) {
        ArrowBackdropScrim()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Daily Challenges",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(44.dp)
                    .pressScale(interactionSource, pressedScale = 0.88f)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 30.dp)
                .gentleFloat(amplitudeDp = 7f, periodMillis = 5000)
                .size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft bloom behind the trophy, so it reads as lit rather than pasted on.
            Box(
                Modifier
                    .pulse(min = 0.92f, max = 1.08f, periodMillis = 3600)
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
            )
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size(104.dp)
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    completedDays: Set<Long>,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val weekdays = remember { listOf("S", "M", "T", "W", "T", "F", "S") }

    // Sunday-first layout, matching the reference.
    val leadingBlanks = remember(month) {
        month.atDay(1).dayOfWeek.value % 7
    }
    val dayCount = remember(month) { month.lengthOfMonth() }
    val rows = remember(leadingBlanks, dayCount) { ((leadingBlanks + dayCount) + 6) / 7 }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.inkMuted.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val index = row * 7 + column
                    val dayOfMonth = index - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayOfMonth in 1..dayCount) {
                            val date = month.atDay(dayOfMonth)
                            DayCell(
                                day = dayOfMonth,
                                isToday = date == today,
                                isPast = date.isBefore(today),
                                isCompleted = date.toEpochDay() in completedDays,
                                // Diagonal stagger: the wave crosses the grid once.
                                delayMillis = Motion.stagger(row + column, step = 26, max = 360)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    isPast: Boolean,
    isCompleted: Boolean,
    delayMillis: Int,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette

    Box(
        modifier = modifier
            .enterFromBelow(delayMillis = delayMillis, travel = 10f, key = day)
            .size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        // Halo sits under the pill so it can breathe past the circle's edge.
        if (isToday) {
            Box(
                Modifier
                    .pulse(min = 1f, max = 1.18f, periodMillis = 2200)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Blue500.copy(alpha = 0.16f))
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isToday -> Blue500
                        isCompleted -> palette.accentGold.copy(alpha = 0.18f)
                        else -> Color.Transparent
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted && !isToday) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Completed",
                    tint = palette.accentGold,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        isToday -> Color.White
                        isPast -> palette.inkSoft
                        else -> palette.inkMuted.copy(alpha = 0.55f)
                    }
                )
            }
        }
    }
}
