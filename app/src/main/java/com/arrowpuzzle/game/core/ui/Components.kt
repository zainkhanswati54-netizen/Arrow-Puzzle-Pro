package com.arrowpuzzle.game.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.motion.pressScale

/**
 * The building blocks every screen is assembled from. Keeping them here means a
 * change to the tap feel or the card elevation lands everywhere at once.
 */

/** The blue pill from the reference — New Game, Play, Accept, OK, Continue. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    brush: Brush? = null
) {
    val palette = AppTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = Motion.smooth(Motion.Quick),
        label = "pillEnabled"
    )

    Box(
        modifier = modifier
            .heightIn(min = AppTheme.spacing.pillHeight)
            .pressScale(interactionSource, pressedScale = 0.965f)
            .graphicsLayer { this.alpha = alpha }
            .shadow(
                elevation = 14.dp,
                shape = CircleShape,
                ambientColor = Color(0xFF2F86F6).copy(alpha = 0.35f),
                spotColor = Color(0xFF2F86F6).copy(alpha = 0.45f)
            )
            .clip(CircleShape)
            .background(brush ?: palette.primaryButton)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 28.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

/** White grouped container. All list rows in the reference live inside one of these. */
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color(0xFF1F2A3D).copy(alpha = 0.10f),
                spotColor = Color(0xFF1F2A3D).copy(alpha = 0.12f)
            ),
        color = AppTheme.palette.surface,
        shape = shape
    ) {
        Column(content = content)
    }
}

/** Coloured rounded-square icon tile used down the left of every settings row. */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Int = 36,
    containerColor: Color = tint,
    iconColor: Color = Color.White,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size((size * 0.56f).dp)
        )
    }
}

/** A single tappable row: tile, label, chevron. */
@Composable
fun SettingsRow(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    emphasized: Boolean = false,
    showChevron: Boolean = true,
    showDivider: Boolean = false
) {
    val palette = AppTheme.palette
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interactionSource, pressedScale = 0.985f)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .defaultMinSize(minHeight = if (emphasized) 88.dp else 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(
                icon = icon,
                tint = tint,
                size = if (emphasized) 56 else 36,
                containerColor = if (emphasized) tint.copy(alpha = 0.16f) else tint,
                iconColor = if (emphasized) tint else Color.White,
                shape = if (emphasized) androidx.compose.foundation.shape.CircleShape
                else RoundedCornerShape(10.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = if (emphasized) MaterialTheme.typography.headlineMedium
                else MaterialTheme.typography.titleMedium,
                color = palette.ink,
                modifier = Modifier.weight(1f)
            )
            trailing?.invoke()
            if (showChevron) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = palette.inkMuted.copy(alpha = 0.65f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (showDivider) {
            Box(
                Modifier
                    .padding(start = 68.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(palette.hairline)
            )
        }
    }
}

/** Small pill used for counters and timers on the home cards. */
@Composable
fun StatPill(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    background: Color = Color.Black.copy(alpha = 0.22f),
    contentColor: Color = Color.White
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

/** Fades content in when [visible] flips, respecting the app's motion vocabulary. */
@Composable
fun SoftFade(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = Motion.smooth(Motion.Normal)),
        exit = fadeOut(animationSpec = Motion.smooth(Motion.Quick))
    ) {
        content()
    }
}
