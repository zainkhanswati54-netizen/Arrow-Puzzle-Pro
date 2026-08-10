package com.arrowpuzzle.game.feature.levels

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Green500
import com.arrowpuzzle.game.core.design.Ink
import com.arrowpuzzle.game.core.game.Difficulty
import com.arrowpuzzle.game.core.game.LevelGenerator
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.ui.AppTopBar
import com.arrowpuzzle.game.core.ui.ArrowBackdrop
import com.arrowpuzzle.game.feature.game.GameViewModel

/** Lightweight per-level summary for the grid — avoids generating full arrow layouts. */
private data class LevelSummary(val id: Int, val difficulty: Difficulty)

/** Total number of levels shown on the select screen (the game itself generates unlimited levels). */
private const val TOTAL_LEVELS = 60

@Composable
fun LevelSelectScreen(
    onLevelSelected: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // The highest level number currently unlocked (same source of truth used by ArrowNavHost).
    val unlockedLevel by GameViewModel.readProgress(context).collectAsState(initial = 1)
    val highestCompleted = (unlockedLevel - 1).coerceAtLeast(0)
    val palette = AppTheme.palette
    val levels = remember { (1..TOTAL_LEVELS).map { LevelSummary(it, LevelGenerator.difficultyFor(it)) } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas)
    ) {
        ArrowBackdrop(tint = Color(0xFFE8EDF4))

        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = "Select Level", onBack = onBack)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Arrow Puzzle Pro",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .enterFromBelow(delayMillis = Motion.stagger(0))
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${highestCompleted} of ${levels.size} completed",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .enterFromBelow(delayMillis = Motion.stagger(1))
            )

            Spacer(Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(levels, key = { it.id }) { level ->
                    val isUnlocked = level.id <= unlockedLevel
                    val isCompleted = level.id < unlockedLevel

                    LevelCard(
                        levelId = level.id,
                        difficulty = level.difficulty.name,
                        isUnlocked = isUnlocked,
                        isCompleted = isCompleted,
                        onClick = {
                            if (isUnlocked) {
                                SoundEngine.playButton()
                                onLevelSelected(level.id)
                            } else {
                                SoundEngine.playError()
                            }
                        },
                        staggerIndex = level.id - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(
    levelId: Int,
    difficulty: String,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    staggerIndex: Int,
    modifier: Modifier = Modifier
) {
    val palette = AppTheme.palette
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .enterFromBelow(delayMillis = Motion.stagger(staggerIndex + 2))
            .aspectRatio(1f)
            .pressScale(interactionSource, pressedScale = if (isUnlocked) 0.94f else 1f)
            .shadow(
                elevation = if (isUnlocked) 10.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Ink.copy(alpha = 0.08f),
                spotColor = Ink.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isUnlocked) palette.surface else palette.canvasSunken
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .graphicsLayer {
                alpha = if (isUnlocked) 1f else 0.65f
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = palette.inkMuted,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = "$levelId",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (isCompleted) Green500 else Blue500
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = if (isUnlocked) difficulty else "Locked",
                style = MaterialTheme.typography.labelSmall,
                color = palette.inkMuted
            )

            if (isCompleted) {
                Spacer(Modifier.height(2.dp))
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Completed",
                    tint = Green500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
