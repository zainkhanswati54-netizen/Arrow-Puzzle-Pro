package com.arrowpuzzle.game.feature.daily

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Blue500
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.pulse
import com.arrowpuzzle.game.core.ui.PrimaryPillButton

private data class IntroPage(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Brush,
    val body: String,
    val cta: String
)

/**
 * The two-card onboarding from the reference. Pages swap with a directional
 * AnimatedContent so the dots and the content agree about which way you're going.
 */
@Composable
fun DailyIntroDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val palette = AppTheme.palette
    var page by remember { mutableIntStateOf(0) }

    val pages = remember {
        listOf(
            IntroPage(
                icon = Icons.Rounded.Star,
                iconTint = Color(0xFFF08000),
                iconBackground = Brush.verticalGradient(
                    listOf(Color(0xFFFFD64D), Color(0xFFFFC02E))
                ),
                body = "Complete a daily challenge every day and win gold stars.",
                cta = "Continue"
            ),
            IntroPage(
                icon = Icons.Rounded.EmojiEvents,
                iconTint = Color(0xFFF5A623),
                iconBackground = Brush.verticalGradient(
                    listOf(Color(0xFFF4F7FC), Color(0xFFE9EFF8))
                ),
                body = "Collect every star in a month and win that month's unique trophy.",
                cta = "OK"
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
                    tonalElevation = 0.dp,
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome to\nDaily Challenges",
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(24.dp))

                        AnimatedContent(
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
                            label = "introPage"
                        ) { index ->
                            val content = pages[index]
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .pulse(min = 0.97f, max = 1.04f, periodMillis = 2800)
                                        .size(180.dp)
                                        .clip(CircleShape)
                                        .background(content.iconBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = content.icon,
                                        contentDescription = null,
                                        tint = content.iconTint,
                                        modifier = Modifier.size(96.dp)
                                    )
                                }

                                Spacer(Modifier.height(26.dp))

                                Text(
                                    text = content.body,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.inkMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        PageDots(count = pages.size, selected = page)

                        Spacer(Modifier.height(22.dp))

                        PrimaryPillButton(
                            text = pages[page].cta,
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

/** Dots that stretch rather than just recolour — cheap, and reads as motion. */
@Composable
private fun PageDots(count: Int, selected: Int, modifier: Modifier = Modifier) {
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
