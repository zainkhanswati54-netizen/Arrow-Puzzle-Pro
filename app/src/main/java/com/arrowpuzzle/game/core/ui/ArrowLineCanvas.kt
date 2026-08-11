package com.arrowpuzzle.game.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.arrowpuzzle.game.core.game.CellKey
import com.arrowpuzzle.game.core.game.Direction

/**
 * Slim "line art" arrow renderer — a thin shaft with a small **rounded**
 * fillet at every turn (matches the competitor reference's corner treatment:
 * not a big pipe-radius bulge, just enough rounding that turns don't look
 * knife-sharp) and a solid filled triangular head.
 *
 * Each cell's arm-run is still built as ONE continuous [Path] (moveTo →
 * lineTo → lineTo …) — that's what keeps the joint clean instead of showing
 * the little square "spike" artifact you get when two butt-capped segments
 * are drawn separately and happen to overlap at a joint. Only the join type
 * changed (Miter → Round); the path construction is untouched.
 */
fun DrawScope.drawArrowLineNetwork(
    cells: Map<CellKey, Direction>,
    cellPx: Float,
    color: Color,
    originX: Float = 0f,
    originY: Float = 0f,
    lineWidthFraction: Float = 0.11f,
    highlight: Map<CellKey, Color> = emptyMap()
) {
    if (cells.isEmpty()) return
    val lineWidth = cellPx * lineWidthFraction
    val stroke = Stroke(lineWidth, cap = StrokeCap.Butt, join = StrokeJoin.Round)
    val armLen = cellPx * 0.5f
    val headLen = cellPx * 0.22f
    val headHalfWidth = cellPx * 0.165f

    for ((cell, dir) in cells) {
        val cellColor = highlight[cell] ?: color
        val cx = originX + cell.col * cellPx + cellPx / 2f
        val cy = originY + cell.row * cellPx + cellPx / 2f
        val arms = armsFor(cell, dir, cells)

        // Shaft: stop short of the tip so the filled head sits flush against
        // the line with no double-thickness overlap at the point.
        val tipX = cx + dir.dx * armLen; val tipY = cy + dir.dy * armLen
        val headBackX = tipX - dir.dx * headLen; val headBackY = tipY - dir.dy * headLen

        val path = Path()
        when {
            arms.size == 2 && arms.contains(dir.opposite) -> {
                val other = dir.opposite
                val farX = cx + other.dx * armLen; val farY = cy + other.dy * armLen
                path.moveTo(farX, farY)
                path.lineTo(headBackX, headBackY)
            }
            arms.size == 2 -> {
                val other = arms.first { it != dir }
                val farX = cx + other.dx * armLen; val farY = cy + other.dy * armLen
                path.moveTo(farX, farY)
                path.lineTo(cx, cy)
                path.lineTo(headBackX, headBackY)
            }
            else -> {
                for (a in arms) {
                    if (a == dir) {
                        path.moveTo(cx, cy)
                        path.lineTo(headBackX, headBackY)
                    } else {
                        path.moveTo(cx, cy)
                        path.lineTo(cx + a.dx * armLen, cy + a.dy * armLen)
                    }
                }
            }
        }
        drawPath(path, cellColor, style = stroke)
        drawArrowHead(tipX, tipY, headBackX, headBackY, dir, headHalfWidth, cellColor)
    }
}

/** Solid filled triangle — always renders crisp since it's a closed fill,
 *  never a stroked chevron that can show a hairline gap at the point. */
private fun DrawScope.drawArrowHead(
    tipX: Float, tipY: Float,
    backX: Float, backY: Float,
    dir: Direction,
    halfWidth: Float,
    color: Color
) {
    val px = -dir.dy.toFloat(); val py = dir.dx.toFloat()
    val head = Path().apply {
        moveTo(tipX, tipY)
        lineTo(backX + px * halfWidth, backY + py * halfWidth)
        lineTo(backX - px * halfWidth, backY - py * halfWidth)
        close()
    }
    drawPath(head, color)
}

/** Standalone single arrow (shaft + head) for the flying tap-clear animation. */
fun DrawScope.drawStandaloneArrowLine(dir: Direction, sizePx: Float, color: Color) {
    val cx = size.width / 2f; val cy = size.height / 2f
    val armLen = sizePx * 0.42f
    val lineWidth = sizePx * 0.10f
    val headLen = sizePx * 0.20f
    val headHalfWidth = sizePx * 0.145f
    val stroke = Stroke(lineWidth, cap = StrokeCap.Butt, join = StrokeJoin.Round)

    val tailX = cx - dir.dx * armLen; val tailY = cy - dir.dy * armLen
    val tipX = cx + dir.dx * armLen; val tipY = cy + dir.dy * armLen
    val headBackX = tipX - dir.dx * headLen; val headBackY = tipY - dir.dy * headLen

    val shaft = Path().apply { moveTo(tailX, tailY); lineTo(headBackX, headBackY) }
    drawPath(shaft, color, style = stroke)
    drawArrowHead(tipX, tipY, headBackX, headBackY, dir, headHalfWidth, color)
}
