package com.arrowpuzzle.game.feature.game

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arrowpuzzle.game.core.ads.AdManager
import com.arrowpuzzle.game.core.ads.BannerAdView
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.data.AppViewModel
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.design.Blue600
import com.arrowpuzzle.game.core.design.BlueDeep
import com.arrowpuzzle.game.core.design.Ink
import com.arrowpuzzle.game.core.design.Red500
import com.arrowpuzzle.game.core.game.CellKey
import com.arrowpuzzle.game.core.game.Direction
import com.arrowpuzzle.game.core.game.Level
import com.arrowpuzzle.game.core.game.PuzzleState
import com.arrowpuzzle.game.core.motion.Haptics
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.motion.pulse
import com.arrowpuzzle.game.core.motion.shakeOnce
import com.arrowpuzzle.game.core.ui.AppTopBar
import com.arrowpuzzle.game.core.ui.ConfettiOverlay
import com.arrowpuzzle.game.core.ui.PrimaryPillButton
import com.arrowpuzzle.game.core.ui.SunburstBackground
import com.arrowpuzzle.game.core.ui.drawArrowLineNetwork
import com.arrowpuzzle.game.core.ui.drawStandaloneArrowLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameScreen(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
    levelId: Int = 1,
    isDaily: Boolean = false,
    appViewModel: AppViewModel? = null,
    onDailyComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val vm: GameViewModel = viewModel(
        key = "game_$levelId",
        factory = GameViewModel.factory(context, levelId, isDaily)
    )
    val ui by vm.state.collectAsState()
    val puzzle = ui.puzzle
    val palette = AppTheme.palette

    // Reward loop: base coins per clear, a bit more for a clean (no-hint,
    // low-move) solve. Computed once per win, not on every recomposition.
    var coinsEarned by remember(puzzle?.level?.id, isDaily) { mutableStateOf(0) }
    var totalCoins by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var streakIncreased by remember(puzzle?.level?.id, isDaily) { mutableStateOf(false) }
    var coinsAlreadyDoubled by remember(puzzle?.level?.id, isDaily) { mutableStateOf(false) }
    val isMilestoneLevel = !isDaily && puzzle != null && puzzle.level.id % 5 == 0

    // Sequences the win flow the way the reference does: board clears → a
    // short "Impressive!" beat while the board sits empty → then the
    // fullscreen celebration. Keyed on the level id so it resets cleanly
    // when nextLevel()/retry() swap in a fresh GameUiState.
    var showImpressive by remember(puzzle?.level?.id, isDaily) { mutableStateOf(false) }
    var revealWin by remember(puzzle?.level?.id, isDaily) { mutableStateOf(false) }

    LaunchedEffect(ui.showWinCelebration, puzzle?.level?.id) {
        if (ui.showWinCelebration) {
            showImpressive = true
            delay(650)
            showImpressive = false
            delay(140)
            revealWin = true
        } else {
            showImpressive = false
            revealWin = false
        }
    }

    // Award coins + update the daily streak exactly once per win, right as
    // the celebration reveals — not on the win *flag* flipping, since that
    // fires before the "Impressive!" beat and would show 0 coins briefly.
    LaunchedEffect(revealWin) {
        if (revealWin && puzzle != null && appViewModel != null) {
            val base = 10 + if (isMilestoneLevel) 40 else 0
            appViewModel.onLevelCompleted(base) { total, streak, increased ->
                coinsEarned = base
                totalCoins = total
                currentStreak = streak
                streakIncreased = increased
            }
        }
    }

    Box(modifier.fillMaxSize().background(palette.canvas)) {
        if (ui.loading || puzzle == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue500)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    title = if (isDaily) "Daily Challenge" else "Level ${puzzle.level.id}",
                    onBack = onExit,
                    trailing = {
                        val src = remember { MutableInteractionSource() }
                        Box(
                            Modifier
                                .size(40.dp)
                                .pressScale(src, 0.88f)
                                .clip(CircleShape)
                                .clickable(src, null, role = Role.Button) {
                                    Haptics.tapButton()
                                    SoundEngine.playButton()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Settings, "Settings", tint = palette.inkMuted, modifier = Modifier.size(22.dp))
                        }
                    }
                )

                // Row 2: remaining-arrows chip · hearts · difficulty chip — mirrors
                // the reference's two-row HUD instead of cramming everything into one.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(text = "${puzzle.remaining.size}", leading = { Text("\uD83C\uDFC1", style = MaterialTheme.typography.labelMedium) })
                    LivesRow(puzzle.lives)
                    StatChip(text = puzzle.level.difficulty.name)
                }

                Spacer(Modifier.weight(0.05f))

                // ── MAZE ──
                MazeBoard(
                    puzzle = puzzle,
                    hintCell = ui.hintCell,
                    onCellTap = { r, c -> vm.onCellTap(r, c) },
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
                )

                Spacer(Modifier.weight(0.05f))

                // Buttons — small circular icon actions, matching the reference.
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                ) {
                    ToolBtn(Icons.Rounded.Lightbulb, "Hint", puzzle.hintsRemaining) {
                        if (puzzle.hintsRemaining > 0) {
                            vm.onHint()
                        } else if (activity != null && AdManager.isHintAdReady()) {
                            Haptics.tapButton()
                            AdManager.showRewardedInterstitialForHint(
                                activity,
                                onEarned = { vm.grantBonusHint() },
                                onUnavailable = { SoundEngine.playError(); Haptics.tapWrong() }
                            )
                        } else {
                            SoundEngine.playError(); Haptics.tapWrong()
                        }
                    }
                    ToolBtn(Icons.Rounded.Refresh, "Retry") {
                        Haptics.tapButton(); SoundEngine.playButton(); vm.retry()
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Anchored banner — sits below the tool row, above the nav bar,
                // so it never overlaps the tap targets on the board itself.
                BannerAdView(Modifier.padding(bottom = 4.dp))
            }
        }

        // Tutorial
        if (puzzle?.level?.isTutorial == true && ui.tutorialStep == 0) TutOverlay { vm.dismissTutorial() }

        // Mid-level praise beat, shown while the board sits empty just before
        // the fullscreen celebration.
        if (showImpressive) ImpressiveToast()

        // Win
        if (revealWin && puzzle != null) {
            WinDlg(
                level = puzzle.level,
                moves = puzzle.moveCount,
                isDaily = isDaily,
                coinsEarned = coinsEarned,
                totalCoins = totalCoins,
                streak = currentStreak,
                streakIncreased = streakIncreased,
                isMilestone = isMilestoneLevel,
                canDoubleCoins = !coinsAlreadyDoubled && activity != null && AdManager.isRewardedAdReady(),
                onDoubleCoins = {
                    val act = activity ?: return@WinDlg
                    AdManager.showRewarded(
                        act,
                        onEarned = {
                            coinsAlreadyDoubled = true
                            appViewModel?.onRewardedCoinsDoubled(coinsEarned) { total -> totalCoins = total }
                        },
                        onUnavailable = {}
                    )
                },
                onNext = {
                    activity?.let { AdManager.maybeShowInterstitialOnLevelComplete(it) }
                    vm.nextLevel()
                },
                onClaim = { onDailyComplete() },
                onExit = {
                    activity?.let { AdManager.maybeShowInterstitialOnLevelComplete(it) }
                    onExit()
                }
            )
        }
        // Game over
        if (ui.showGameOver) {
            GameOverDlg(
                canContinueWithAd = activity != null && AdManager.isRewardedAdReady(),
                onContinueWithAd = {
                    val act = activity ?: return@GameOverDlg
                    AdManager.showRewarded(act, onEarned = { vm.retry() }, onUnavailable = { vm.retry() })
                },
                onRetry = { vm.retry() },
                onExit = onExit
            )
        }
    }
}

// ── MAZE BOARD ───────────────────────────────────────────────────────────────

private class ExitAnim(val direction: Direction, val progress: Animatable<Float, AnimationVector1D>)

@Composable
private fun MazeBoard(puzzle: PuzzleState, hintCell: CellKey?, onCellTap: (Int, Int) -> Unit, modifier: Modifier) {
    val level = puzzle.level
    val ghostCells = remember(level) { level.arrows.associate { CellKey(it.row, it.col) to it.direction } }
    val scope = rememberCoroutineScope()

    // Flying-off tap-clear animation state, keyed per level so it resets on
    // retry/next level. One entry per cell currently mid-exit.
    val exitAnims = remember(level) { mutableStateMapOf<CellKey, ExitAnim>() }
    var lastClearedCount by remember(level) { mutableStateOf(puzzle.cleared.size) }

    LaunchedEffect(puzzle.cleared.size) {
        if (puzzle.cleared.size > lastClearedCount) {
            val newCell = puzzle.cleared.last()
            val dir = ghostCells[newCell]
            if (dir != null && newCell !in exitAnims) {
                val anim = Animatable(0f)
                exitAnims[newCell] = ExitAnim(dir, anim)
                scope.launch {
                    // Slowed and softened from the original quick "yeet off
                    // screen" — a longer, evenly-eased slide reads as more
                    // deliberate and lets the player actually see the arrow
                    // travel its path instead of just vanishing.
                    anim.animateTo(1f, tween(650, easing = Motion.Standard))
                    exitAnims.remove(newCell)
                }
            }
        }
        lastClearedCount = puzzle.cleared.size
    }

    // Brief red flash on a blocked tap — the rule engine already tracked
    // lastError, it just wasn't surfaced visually before. Now smoothly
    // animated (fade in/out) instead of an abrupt show/hide, and the flash
    // token changes on every error so shakeOnce() re-fires even if the same
    // cell is mis-tapped twice in a row.
    var shakeCell by remember(level) { mutableStateOf<CellKey?>(null) }
    var errorToken by remember(level) { mutableStateOf(0) }
    val errorFlash = remember(level) { Animatable(0f) }
    LaunchedEffect(puzzle.lastError) {
        if (puzzle.lastError != null) {
            shakeCell = puzzle.lastError
            errorToken++
            errorFlash.snapTo(1f)
            errorFlash.animateTo(0f, tween(360, easing = Motion.Exit))
            if (shakeCell == puzzle.lastError) shakeCell = null
        }
    }

    // Level-start reveal — board scales/fades in from the centre, keyed on
    // the level id, mirroring the competitor reference's board-appear beat
    // instead of the maze just snapping into existence.
    val boardEnter = remember(level) { Animatable(0f) }
    LaunchedEffect(level) {
        boardEnter.snapTo(0f)
        boardEnter.animateTo(1f, tween(Motion.Slow, easing = Motion.Emphasized))
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cellW = maxWidth / level.gridCols
        val cellH = maxHeight / level.gridRows
        val cellSize = min(cellW, cellH).coerceAtMost(64.dp)
        val bw = cellSize * level.gridCols
        val bh = cellSize * level.gridRows

        Box(
            Modifier
                .width(bw).height(bh)
                .graphicsLayer {
                    val p = boardEnter.value
                    alpha = p
                    val s = 0.85f + 0.15f * p
                    scaleX = s; scaleY = s
                }
                .shakeOnce(errorToken.takeIf { it > 0 })
        ) {
            // Ghost layer — the full original board at low opacity. Once most
            // arrows are cleared this is what keeps the board from looking
            // broken/blank: the maze silhouette stays on screen throughout.
            Canvas(Modifier.fillMaxSize()) {
                drawArrowLineNetwork(ghostCells, cellSize.toPx(), Ink.copy(alpha = 0.10f))
            }

            // Hint glow, drawn under the active pipe so the pipe reads on top.
            hintCell?.let { hc ->
                Box(
                    Modifier
                        .offset(cellSize * hc.col, cellSize * hc.row)
                        .size(cellSize)
                        .pulse(min = 0.85f, max = 1.05f, periodMillis = 900)
                        .background(Blue500.copy(alpha = 0.28f), CircleShape)
                )
            }
            // Blocked-tap flash — soft red glow behind the cell, fading out
            // smoothly rather than popping on/off.
            shakeCell?.let { sc ->
                Box(
                    Modifier
                        .offset(cellSize * sc.col, cellSize * sc.row)
                        .size(cellSize)
                        .graphicsLayer { alpha = errorFlash.value }
                        .background(Red500.copy(alpha = 0.30f), CircleShape)
                )
            }

            // Active layer — only arrows still in play, full ink, redrawn
            // whenever the remaining set changes (i.e. once per tap). The
            // cell that just triggered a blocked tap renders in red for the
            // duration of the flash, same "path turns red" tell the
            // competitor reference uses.
            Canvas(Modifier.fillMaxSize()) {
                val highlight = shakeCell?.let { sc ->
                    mapOf(sc to lerp(Ink, Red500, errorFlash.value))
                } ?: emptyMap()
                drawArrowLineNetwork(puzzle.remaining, cellSize.toPx(), Ink, highlight = highlight)
            }

            // Flying-off arrows: tapped cells animate off-board in their
            // pointing direction with a stretch trail and a colour shift to
            // blue, instead of just disappearing in place.
            exitAnims.forEach { (cell, exit) ->
                key(cell) {
                    Box(
                        Modifier
                            .offset(cellSize * cell.col, cellSize * cell.row)
                            .size(cellSize)
                            .graphicsLayer {
                                val p = exit.progress.value
                                val cellPx = cellSize.toPx()
                                val dist = cellPx * (level.gridCols + level.gridRows) * 0.65f
                                translationX = exit.direction.dx * p * dist
                                translationY = exit.direction.dy * p * dist
                                // Quick punch-pop in the first ~12% of the exit before the
                                // stretch-and-fly takes over — reads as an immediate,
                                // satisfying "hit" on tap instead of a delayed slide.
                                val punch = if (p < 0.12f) {
                                    1f + 0.32f * kotlin.math.sin((p / 0.12f) * Math.PI.toFloat())
                                } else 1f
                                val stretch = 1f + p * 1.4f
                                if (exit.direction.dx != 0) {
                                    scaleX = stretch * punch; scaleY = punch
                                } else {
                                    scaleY = stretch * punch; scaleX = punch
                                }
                                alpha = (1f - ((p - 0.5f).coerceAtLeast(0f) / 0.5f)).coerceIn(0f, 1f)
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val col = lerp(Ink, Blue500, (exit.progress.value / 0.3f).coerceIn(0f, 1f))
                            drawStandaloneArrowLine(exit.direction, size.minDimension, col)
                        }
                    }
                }
            }

            // Invisible tap targets — one per remaining cell.
            puzzle.remaining.keys.forEach { cell ->
                key(cell) {
                    val src = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .offset(cellSize * cell.col, cellSize * cell.row)
                            .size(cellSize)
                            .clickable(src, null, role = Role.Button) { onCellTap(cell.row, cell.col) }
                    )
                }
            }
        }
    }
}

// ── UI COMPONENTS ────────────────────────────────────────────────────────────

@Composable
private fun LivesRow(lives: Int) = Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    repeat(3) { Icon(Icons.Rounded.Favorite, null, tint = if (it < lives) Red500 else Red500.copy(0.22f), modifier = Modifier.size(18.dp)) }
}

/** Small pill chip used for the remaining-arrows count and the difficulty label. */
@Composable
private fun StatChip(text: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val pal = AppTheme.palette
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(pal.canvasSunken)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        leading?.invoke()
        Text(text, style = MaterialTheme.typography.labelMedium, color = pal.inkSoft)
    }
}

/** Small circular icon action button — Hint / Retry, matching the reference's compact tool row. */
@Composable
private fun ToolBtn(icon: ImageVector, label: String, badge: Int? = null, onClick: () -> Unit) {
    val pal = AppTheme.palette
    val src = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(56.dp)
            .pressScale(src, 0.9f)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(pal.surface)
            .clickable(src, null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = Blue500, modifier = Modifier.size(24.dp))
        if (badge != null && badge > 0) {
            Box(
                Modifier.align(Alignment.TopEnd).offset((-2).dp, 2.dp).size(18.dp).clip(CircleShape).background(Blue500),
                contentAlignment = Alignment.Center
            ) {
                Text("$badge", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color.White)
            }
        }
    }
}

@Composable
private fun TutOverlay(onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.55f))
        .clickable(remember { MutableInteractionSource() }, null) { SoundEngine.playButton(); onDismiss() }, Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.enterFromBelow(travel = 30f).padding(40.dp)) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.pulse().size(48.dp))
            Spacer(Modifier.height(20.dp))
            Text("Tap arrows to escape!", style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Tap an arrow to send it flying off the board.\nIt only moves if nothing blocks the path!\nClear all arrows to complete the level.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(0.82f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Text("Tap anywhere to start", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.55f))
        }
    }
}

/** Short praise beat shown while the board sits empty, right before the
 *  fullscreen celebration — mirrors the reference's "Impressive!" card. */
@Composable
private fun ImpressiveToast() {
    val phrases = remember { listOf("Impressive!", "Nicely done!", "Smooth clear!") }
    val phrase = remember { phrases.random() }
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        val s = remember { MutableTransitionState(false) }; s.targetState = true
        AnimatedVisibility(
            s,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.7f, animationSpec = Motion.playful()),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.85f)
        ) {
            Surface(shape = RoundedCornerShape(20.dp), color = AppTheme.palette.surface, shadowElevation = 10.dp) {
                Column(Modifier.padding(24.dp, 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDE32", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(6.dp))
                    Text(phrase, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Blue500)
                }
            }
        }
    }
}

/** Small solved-board preview shown on the win card — draws the level's full
 *  original pipe network at a fixed thumbnail scale. */
@Composable
private fun LevelThumbnail(level: Level, sizeDp: Dp) {
    val cells = remember(level) { level.arrows.associate { CellKey(it.row, it.col) to it.direction } }
    val cellPx = with(LocalDensity.current) { sizeDp.toPx() / maxOf(level.gridRows, level.gridCols) }
    Canvas(Modifier.size(sizeDp)) {
        drawArrowLineNetwork(cells, cellPx, Ink)
    }
}

@Composable
private fun WinDlg(
    level: Level,
    moves: Int,
    isDaily: Boolean,
    coinsEarned: Int,
    totalCoins: Int,
    streak: Int,
    streakIncreased: Boolean,
    isMilestone: Boolean,
    canDoubleCoins: Boolean,
    onDoubleCoins: () -> Unit,
    onNext: () -> Unit,
    onClaim: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onExit, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Blue500, Blue600, BlueDeep)))
        ) {
            SunburstBackground(Modifier.fillMaxSize())
            ConfettiOverlay(Modifier.fillMaxSize())

            val s = remember { MutableTransitionState(false) }; s.targetState = true
            Column(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    when {
                        isDaily -> "Daily Star Earned!"
                        isMilestone -> "Milestone Reached! 🎁"
                        else -> "Level Completed!"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                if (!isDaily && coinsEarned > 0) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🪙", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "+$coinsEarned coins  ·  $totalCoins total",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                    if (streak > 1) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (streakIncreased) "🔥 $streak-day streak!" else "🔥 $streak-day streak",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                AnimatedVisibility(
                    s,
                    enter = fadeIn(tween(320)) + scaleIn(initialScale = 0.7f, animationSpec = Motion.playful())
                ) {
                    Surface(shape = RoundedCornerShape(26.dp), color = Color.White, shadowElevation = 18.dp) {
                        Box(Modifier.padding(28.dp), Alignment.Center) {
                            LevelThumbnail(level, 128.dp)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (moves == 1) "Cleared in 1 move" else "Cleared in $moves moves",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (canDoubleCoins) {
                    Spacer(Modifier.height(18.dp))
                    val src = remember { MutableInteractionSource() }
                    Row(
                        Modifier
                            .pressScale(src, 0.965f)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .clickable(src, null, role = Role.Button) {
                                SoundEngine.playButton(); onDoubleCoins()
                            }
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎬", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Watch ad to double coins (+$coinsEarned)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                if (isDaily) {
                    WhitePillButton("Claim Star", onClaim, Modifier.fillMaxWidth())
                } else {
                    WhitePillButton("Next Game", onNext, Modifier.fillMaxWidth(), subtitle = "Level ${level.id + 1}")
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Main",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.clip(CircleShape).clickable {
                        SoundEngine.playButton(); onExit()
                    }.padding(16.dp, 8.dp)
                )
            }
        }
    }
}

/** White pill with blue text — the win screen's primary action sits on a
 *  blue background, so it inverts the usual blue-pill/white-text button. */
@Composable
private fun WhitePillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null) {
    val src = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(src, 0.965f)
            .shadow(10.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(src, null, role = Role.Button) {
                Haptics.tapButton(); onClick()
            }
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Blue600)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Blue600.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun GameOverDlg(
    canContinueWithAd: Boolean,
    onContinueWithAd: () -> Unit,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    Dialog(onExit, DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.45f)).padding(28.dp), Alignment.Center) {
            val s = remember { MutableTransitionState(false) }; s.targetState = true
            AnimatedVisibility(s, enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.85f, animationSpec = Motion.bouncy()), exit = fadeOut(tween(200))) {
                Surface(shape = RoundedCornerShape(26.dp), color = AppTheme.palette.surface, shadowElevation = 24.dp) {
                    Column(Modifier.padding(24.dp, 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Out of lives!", style = MaterialTheme.typography.headlineMedium, color = AppTheme.palette.ink)
                        Spacer(Modifier.height(8.dp))
                        Text("Only tap arrows with a clear path ahead!", style = MaterialTheme.typography.bodyLarge, color = AppTheme.palette.inkMuted, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(28.dp))
                        if (canContinueWithAd) {
                            val src = remember { MutableInteractionSource() }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .pressScale(src, 0.97f)
                                    .clip(RoundedCornerShape(50))
                                    .background(Blue500.copy(alpha = 0.12f))
                                    .clickable(src, null, role = Role.Button) { SoundEngine.playButton(); onContinueWithAd() }
                                    .padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎬  ", style = MaterialTheme.typography.titleMedium)
                                Text("Watch ad for a fresh set of lives", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Blue500)
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        PrimaryPillButton("Try Again", onRetry, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text("Menu", style = MaterialTheme.typography.titleMedium, color = Blue500,
                            modifier = Modifier.clip(CircleShape).clickable { onExit() }.padding(16.dp, 8.dp))
                    }
                }
            }
        }
    }
}
