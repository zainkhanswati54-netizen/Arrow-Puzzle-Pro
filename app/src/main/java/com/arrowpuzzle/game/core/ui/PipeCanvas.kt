package com.arrowpuzzle.game.core.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.arrowpuzzle.game.core.game.CellKey
import com.arrowpuzzle.game.core.game.Direction

/**
 * Renders the board as one connected pipe/track instead of separate per-cell
 * glyphs — this is the "connected-maze" look from the competitor reference.
 *
 * Connection rule: a cell always lays track in the direction its own arrow
 * points, and picks up an extra arm for every neighbour whose arrow aims
 * *into* it. Two opposite arms draw as a straight run; two perpendicular arms
 * draw as a rounded elbow; 1 arm (a dead end) or 3-4 arms (a junction) draw as
 * straight stubs from the cell centre. This is what keeps a ~95%+ filled
 * board reading as corridors rather than a solid grid lattice.
 */
fun armsFor(cell: CellKey, dir: Direction, cells: Map<CellKey, Direction>): List<Direction> {
    val arms = ArrayList<Direction>(4)
    arms.add(dir)
    for (d in Direction.entries) {
        if (d == dir) continue
        val neighbor = CellKey(cell.row + d.dy, cell.col + d.dx)
        val nDir = cells[neighbor] ?: continue
        if (nDir == d.opposite) arms.add(d)
    }
    return arms
}

/**
 * Draws the pipe network for [cells] into this [DrawScope].
 *
 * @param cellPx pixel size of one grid cell
 * @param originX/originY offsets the whole network — lets the ghost layer and
 *   the active layer share exact grid alignment
 */
fun DrawScope.drawPipeNetwork(
    cells: Map<CellKey, Direction>,
    cellPx: Float,
    color: Color,
    originX: Float = 0f,
    originY: Float = 0f,
    pipeWidthFraction: Float = 0.30f
) {
    if (cells.isEmpty()) return
    val pipeWidth = cellPx * pipeWidthFraction
    val stroke = Stroke(pipeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val headStroke = Stroke(pipeWidth * 0.86f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val armLen = cellPx * 0.5f

    for ((cell, dir) in cells) {
        val cx = originX + cell.col * cellPx + cellPx / 2f
        val cy = originY + cell.row * cellPx + cellPx / 2f
        val arms = armsFor(cell, dir, cells)

        val path = Path()
        when {
            arms.size == 2 && arms.contains(dir.opposite) -> {
                val p1 = Offset(cx + dir.dx * armLen, cy + dir.dy * armLen)
                val p2 = Offset(cx - dir.dx * armLen, cy - dir.dy * armLen)
                path.moveTo(p1.x, p1.y); path.lineTo(p2.x, p2.y)
            }
            arms.size == 2 -> {
                val other = arms.first { it != dir }
                val p1 = Offset(cx + dir.dx * armLen, cy + dir.dy * armLen)
                val p2 = Offset(cx + other.dx * armLen, cy + other.dy * armLen)
                path.moveTo(p1.x, p1.y)
                path.quadraticTo(cx, cy, p2.x, p2.y)
            }
            else -> {
                for (a in arms) {
                    path.moveTo(cx, cy)
                    path.lineTo(cx + a.dx * armLen, cy + a.dy * armLen)
                }
            }
        }
        drawPath(path, color, style = stroke)

        // Arrowhead at the tip of this cell's own direction only.
        val tipDist = armLen * 0.94f
        val tipX = cx + dir.dx * tipDist; val tipY = cy + dir.dy * tipDist
        val hs = cellPx * 0.16f
        val px = -dir.dy.toFloat(); val py = dir.dx.toFloat()
        val backX = cx + dir.dx * (tipDist - hs * 1.2f); val backY = cy + dir.dy * (tipDist - hs * 1.2f)
        val head = Path().apply {
            moveTo(backX + px * hs, backY + py * hs)
            lineTo(tipX, tipY)
            lineTo(backX - px * hs, backY - py * hs)
        }
        drawPath(head, color, style = headStroke)
    }
}

/** A standalone single arrow (shaft + head), used for the flying tap-clear
 *  animation where a cell is no longer part of the connected network. */
fun DrawScope.drawStandaloneArrow(dir: Direction, sizePx: Float, color: Color) {
    val cx = size.width / 2f; val cy = size.height / 2f
    val armLen = sizePx * 0.42f
    val pipeWidth = sizePx * 0.22f
    val stroke = Stroke(pipeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val tailX = cx - dir.dx * armLen; val tailY = cy - dir.dy * armLen
    val tipDist = armLen * 0.9f
    val tipX = cx + dir.dx * tipDist; val tipY = cy + dir.dy * tipDist
    val shaft = Path().apply { moveTo(tailX, tailY); lineTo(tipX, tipY) }
    drawPath(shaft, color, style = stroke)
    val hs = sizePx * 0.19f
    val px = -dir.dy.toFloat(); val py = dir.dx.toFloat()
    val backX = cx + dir.dx * (tipDist - hs * 1.2f); val backY = cy + dir.dy * (tipDist - hs * 1.2f)
    val head = Path().apply {
        moveTo(backX + px * hs, backY + py * hs)
        lineTo(cx + dir.dx * armLen, cy + dir.dy * armLen)
        lineTo(backX - px * hs, backY - py * hs)
    }
    drawPath(head, color, style = Stroke(pipeWidth * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
