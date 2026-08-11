package com.arrowpuzzle.game.feature.splash

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arrowpuzzle.game.R
import com.arrowpuzzle.game.core.design.AppTheme
import com.arrowpuzzle.game.core.design.Palette
import com.arrowpuzzle.game.core.motion.Motion
import com.arrowpuzzle.game.core.ui.ArrowBackdrop
import kotlinx.coroutines.delay

/**
 * The Mentric Studios bumper plays first — a few seconds of held attention before
 * the app mark itself appears. On reduced-motion, or if the video ever fails to
 * load, we skip straight to the mark so nobody is ever stuck on a blank frame.
 */
@Composable
fun SplashScreen(
    onReady: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Long = 900,
    studioVideoRes: Int = R.raw.mentric_studios_intro
) {
    val palette = AppTheme.palette
    val reduced = AppTheme.reducedMotion
    val currentOnReady by rememberUpdatedState(onReady)
    var studioIntroDone by remember { mutableStateOf(reduced) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!studioIntroDone) {
            StudioIntroVideo(
                videoRes = studioVideoRes,
                onFinished = { studioIntroDone = true }
            )
        } else {
            AppMark(
                palette = palette,
                reduced = reduced,
                holdMillis = holdMillis,
                onReady = { currentOnReady() }
            )
        }
    }
}

/** Plays the bundled studio bumper once, full-bleed, no controls. */
@Composable
private fun StudioIntroVideo(
    videoRes: Int,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/$videoRes")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentOnFinished()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Never let a codec/device quirk strand the player on a black screen.
                currentOnFinished()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Safety net: if playback never reaches STATE_ENDED on some device, move on anyway.
    LaunchedEffect(Unit) {
        delay(6000)
        currentOnFinished()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PlayerView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
                // Fill the whole screen (crop instead of letterbox) so the bumper
                // never shows black bars on phones whose aspect ratio differs
                // from the source video.
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                player = exoPlayer
            }
        }
    )
}

/** The mark springs in over the arrow field, then hands off with a crossfade. */
@Composable
private fun AppMark(
    palette: Palette,
    reduced: Boolean,
    holdMillis: Long,
    onReady: () -> Unit
) {
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
        onReady()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.canvas),
        contentAlignment = Alignment.Center
    ) {
        ArrowBackdrop(tint = Color(0xFFE9EEF5))

        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
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
