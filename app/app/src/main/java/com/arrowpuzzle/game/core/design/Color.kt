package com.arrowpuzzle.game.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Every colour in the app is sampled from the reference screens. Nothing is invented,
 * and nothing is hard-coded at the call site — screens read from [LocalPalette] so a
 * future dark theme is a single swap rather than a find-and-replace.
 */

// Brand blues -----------------------------------------------------------------
val Blue100 = Color(0xFFDCEBFE)
val Blue300 = Color(0xFF7FB6FA)
val Blue400 = Color(0xFF4A9BFF)
val Blue500 = Color(0xFF2F86F6)
val Blue600 = Color(0xFF1E76EA)
val Blue700 = Color(0xFF1462CE)
val BlueDeep = Color(0xFF0E4FA8)

// Neutrals --------------------------------------------------------------------
val Canvas = Color(0xFFF4F7FB)
val CanvasSunken = Color(0xFFEBF0F7)
val SurfaceWhite = Color(0xFFFFFFFF)
val Ink = Color(0xFF1F2A3D)
val InkSoft = Color(0xFF41506B)
val InkMuted = Color(0xFF8C97AA)
val Hairline = Color(0xFFEDF0F5)

// Accents ---------------------------------------------------------------------
val Amber300 = Color(0xFFFFD64D)
val Amber500 = Color(0xFFF5A623)
val Amber700 = Color(0xFFF08000)
val Orange400 = Color(0xFFF3A055)
val Orange600 = Color(0xFFE2760F)
val Green500 = Color(0xFF22C55E)
val Lime500 = Color(0xFF8BC53F)
val Indigo500 = Color(0xFF5B4FE0)
val Teal500 = Color(0xFF17BEBB)
val Red500 = Color(0xFFF04438)

/** Brand values Material 3's [androidx.compose.material3.ColorScheme] has no slot for. */
@Immutable
data class Palette(
    val canvas: Color = Canvas,
    val canvasSunken: Color = CanvasSunken,
    val surface: Color = SurfaceWhite,
    val ink: Color = Ink,
    val inkSoft: Color = InkSoft,
    val inkMuted: Color = InkMuted,
    val hairline: Color = Hairline,
    val accentGold: Color = Amber500,
    val accentGoldDeep: Color = Amber700,
    val danger: Color = Red500,
) {
    val primaryButton: Brush = Brush.verticalGradient(listOf(Blue400, Blue600))

    val primaryButtonPressed: Brush = Brush.verticalGradient(listOf(Blue500, Blue700))

    val dailyCardBrush: Brush = Brush.linearGradient(listOf(Blue300, Blue500))

    val tournamentCardBrush: Brush = Brush.linearGradient(listOf(Orange400, Orange600))

    val dailyHeaderBrush: Brush = Brush.verticalGradient(listOf(Blue500, Blue600, BlueDeep))

    val goldBrush: Brush = Brush.verticalGradient(listOf(Amber300, Amber500))
}

val LocalPalette = staticCompositionLocalOf { Palette() }
