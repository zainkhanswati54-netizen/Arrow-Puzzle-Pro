package com.arrowpuzzle.game.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.enterFromBelow
import com.arrowpuzzle.game.core.motion.gentleFloat
import com.arrowpuzzle.game.core.motion.pressScale
import com.arrowpuzzle.game.core.motion.pulse
import com.arrowpuzzle.game.core.motion.shimmer

/** Centered title with an optional back affordance, matching the "Me" header. */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    contentColor: Color = AppTheme.palette.ink,
    trailing: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor
        )
        if (onBack != null) {
            val interactionSource = remember { MutableInteractionSource() }
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
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (trailing != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) { trailing() }
        }
    }
}

/** Small shimmering pill. Used inline wherever a control exists but does nothing yet. */
@Composable
fun ComingSoonBadge(
    modifier: Modifier = Modifier,
    text: String = "Coming soon"
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        AppTheme.palette.accentGold.copy(alpha = 0.18f),
                        AppTheme.palette.accentGoldDeep.copy(alpha = 0.10f)
                    )
                )
            )
            .shimmer(highlight = Color.White.copy(alpha = 0.35f), periodMillis = 2600)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = AppTheme.palette.accentGoldDeep,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.palette.accentGoldDeep
        )
    }
}

/**
 * The placeholder every unbuilt destination lands on. Deliberately not an empty
 * grey box: it names the feature, says plainly what it will do, and gives the
 * player a way back — so a half-built app still reads as finished-in-progress.
 */
@Composable
fun ComingSoonScreen(
    title: String,
    headline: String,
    body: String,
    icon: ImageVector,
    accent: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    primaryAction: (@Composable () -> Unit)? = null
) {
    val palette = AppTheme.palette

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvasSunken)
    ) {
        ArrowBackdrop(tint = Color(0xFFE1E8F2).copy(alpha = 0.85f))

        Column(Modifier.fillMaxSize()) {
            AppTopBar(title = title, onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .enterFromBelow(delayMillis = Motion.stagger(0), travel = 18f)
                        .gentleFloat(amplitudeDp = 5f)
                        .size(132.dp)
                        .clip(RoundedCornerShape(38.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.05f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .pulse(min = 0.96f, max = 1.05f)
                            .size(60.dp)
                    )
                }

                Spacer(Modifier.height(28.dp))

                ComingSoonBadge(
                    modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(1))
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(2))
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.inkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.enterFromBelow(delayMillis = Motion.stagger(3))
                )

                if (primaryAction != null) {
                    Spacer(Modifier.height(32.dp))
                    Box(Modifier.enterFromBelow(delayMillis = Motion.stagger(4))) {
                        primaryAction()
                    }
                }

                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun ComingSoonRowTrailing(modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        ComingSoonBadge()
        Spacer(Modifier.width(8.dp))
    }
}
