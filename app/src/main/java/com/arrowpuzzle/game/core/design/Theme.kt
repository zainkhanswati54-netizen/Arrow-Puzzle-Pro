package com.arrowpuzzle.game.core.design

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = SurfaceWhite,
    primaryContainer = Blue100,
    onPrimaryContainer = BlueDeep,
    secondary = Amber500,
    onSecondary = SurfaceWhite,
    background = Canvas,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = CanvasSunken,
    onSurfaceVariant = InkMuted,
    outline = Hairline,
    error = Red500
)

/**
 * True when the user has turned animations off at the OS level. Every animated
 * component in the app reads this and degrades to an instant state change rather
 * than ignoring the setting — accessibility is part of feeling premium.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun ArrowPuzzleTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    }

    CompositionLocalProvider(
        LocalPalette provides Palette(),
        LocalSpacing provides Spacing(),
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Shorthands so screens read `AppTheme.palette.ink` instead of a local lookup. */
object AppTheme {
    val palette: Palette
        @Composable get() = LocalPalette.current
    val spacing: Spacing
        @Composable get() = LocalSpacing.current
    val reducedMotion: Boolean
        @Composable get() = LocalReducedMotion.current
}
