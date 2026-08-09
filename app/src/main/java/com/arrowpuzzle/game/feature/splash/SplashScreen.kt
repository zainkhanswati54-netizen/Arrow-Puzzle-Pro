package com.arrowpuzzle.game.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arrowpuzzle.game.R
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.ui.ArrowBackdrop
import kotlinx.coroutines.delay

/**
 * Two seconds of held attention, spent well: the arrow field is already drifting
 * when the first frame lands, and the mark springs in over it. The handoff to the
 * next screen is a crossfade, so there is never a white flash.
 */
@Composable
fun SplashScreen(
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 1400
) {
    val palette = AppTheme.palette
    val reduced = AppTheme.reducedMotion
    val currentOnReady by rememberUpdatedState(onReady)

    val markScale = remember { Animatable(if (reduced) 1f else 0.62f) }
    val markAlpha = remember { Animatable(if (reduced) 1f else 0f) }
    val glow = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!reduced) {
            markAlpha.animateTo(1f, tween(Motion.Normal, easing = Motion.Emphasized))
        }
        if (!reduced) {
            markScale.animateTo(1f, Motion.playful())
            glow.animateTo(1f, tween(Motion.Slow, easing = Motion.Standard))
        }
        delay(holdMillis)
        currentOnReady()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.canvas),
        contentAlignment = Alignment.Center
    ) {
        ArrowBackdrop(tint = Color(0xFFE9EEF5))

        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = null,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = markScale.value
                    scaleY = markScale.value
                    alpha = markAlpha.value
                }
                .shadow(
                    elevation = 26.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = Color(0xFF2F86F6).copy(alpha = 0.30f),
                    spotColor = Color(0xFF2F86F6).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .size(104.dp)
        )
    }
}
