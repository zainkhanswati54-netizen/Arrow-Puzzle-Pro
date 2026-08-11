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
 * Slim, sharp-cornered "line art" arrow renderer — replaces the thick rounded
 * pipe look with the clean, minimal single-stroke style the team standardised
 * on: a thin shaft, a crisp 90-degree miter at every turn (no rounding, no
 * overlap notches at the joints) and a solid filled triangular head.
 *
 * Each cell's arm-run is built as ONE continuous [Path] (moveTo → lineTo →
 * lineTo …) rather than several overlapping segments — that is what keeps
 * corners perfectly sharp instead of showing the little square "spike"
 * artifact you get when two butt-capped segments are drawn separately and
 * happen to overlap at a joint.
 */
fun DrawScope.drawArrowLineNetwork(
    cells: Map<CellKey, Direction>,
    cellPx: Float,
    color: Color,
    originX: Float = 0f,
    originY: Float = 0f,
    lineWidthFraction: Float = 0.085f
) {
    if (cells.isEmpty()) return
    val lineWidth = cellPx * lineWidthFraction
    val stroke = Stroke(lineWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter, miter = 4f)
    val armLen = cellPx * 0.5f
    val headLen = cellPx * 0.22f
    val headHalfWidth = cellPx * 0.155f

    for ((cell, dir) in cells) {
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
        drawPath(path, color, style = stroke)
        drawArrowHead(tipX, tipY, headBackX, headBackY, dir, headHalfWidth, color)
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
    val lineWidth = sizePx * 0.075f
    val headLen = sizePx * 0.20f
    val headHalfWidth = sizePx * 0.135f
    val stroke = Stroke(lineWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter, miter = 4f)

    val tailX = cx - dir.dx * armLen; val tailY = cy - dir.dy * armLen
    val tipX = cx + dir.dx * armLen; val tipY = cy + dir.dy * armLen
    val headBackX = tipX - dir.dx * headLen; val headBackY = tipY - dir.dy * headLen

    val shaft = Path().apply { moveTo(tailX, tailY); lineTo(headBackX, headBackY) }
    drawPath(shaft, color, style = stroke)
    drawArrowHead(tipX, tipY, headBackX, headBackY, dir, headHalfWidth, color)
}
