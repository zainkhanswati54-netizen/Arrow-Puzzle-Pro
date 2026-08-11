package com.arrowpuzzle.game.core.game

import androidx.compose.runtime.Immutable
import kotlin.random.Random

enum class Direction(val dx: Int, val dy: Int, val degrees: Float) {
    Up(0, -1, -90f), Right(1, 0, 0f), Down(0, 1, 90f), Left(-1, 0, 180f);

    val opposite: Direction
        get() = when (this) {
            Up -> Down; Down -> Up; Left -> Right; Right -> Left
        }
}

enum class Difficulty { Tutorial, Easy, Normal, Hard }
@Immutable data class CellKey(val row: Int, val col: Int)
@Immutable data class ArrowDef(val row: Int, val col: Int, val direction: Direction)

@Immutable
data class Level(
    val id: Int, val gridRows: Int, val gridCols: Int,
    val arrows: List<ArrowDef>, val difficulty: Difficulty,
    val isTutorial: Boolean = false
)

@Immutable
data class PuzzleState(
    val level: Level,
    val remaining: Map<CellKey, Direction>,
    val cleared: List<CellKey> = emptyList(),
    val moveCount: Int = 0,
    val lives: Int = 3,
    val hintsRemaining: Int = 3,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val lastError: CellKey? = null
)

/** Shared by the engine and the generator: is [cell]'s ray to the border clear
 *  of every other cell present in [dirMap]? */
internal fun isEscapable(cell: CellKey, dirMap: Map<CellKey, Direction>, rows: Int, cols: Int): Boolean {
    val dir = dirMap[cell] ?: return false
    var r = cell.row + dir.dy; var c = cell.col + dir.dx
    while (r in 0 until rows && c in 0 until cols) {
        if (CellKey(r, c) in dirMap) return false
        r += dir.dy; c += dir.dx
    }
    return true
}

// ── Engine ───────────────────────────────────────────────────────────────────

object PuzzleEngine {
    fun create(level: Level): PuzzleState =
        PuzzleState(level = level, remaining = level.arrows.associate { CellKey(it.row, it.col) to it.direction })

    fun canEscape(state: PuzzleState, cell: CellKey): Boolean =
        isEscapable(cell, state.remaining, state.level.gridRows, state.level.gridCols)

    fun tap(state: PuzzleState, cell: CellKey): PuzzleState {
        if (state.isComplete || state.isGameOver || cell !in state.remaining) return state
        return if (canEscape(state, cell)) {
            val r = state.remaining - cell
            state.copy(remaining = r, cleared = state.cleared + cell,
                moveCount = state.moveCount + 1, isComplete = r.isEmpty(), lastError = null)
        } else {
            val l = state.lives - 1
            state.copy(lives = l, moveCount = state.moveCount + 1, isGameOver = l <= 0, lastError = cell)
        }
    }

    fun findHint(state: PuzzleState): CellKey? = state.remaining.keys.firstOrNull { canEscape(state, it) }

    fun useHint(state: PuzzleState): PuzzleState {
        if (state.hintsRemaining <= 0) return state
        val t = findHint(state) ?: return state
        val r = state.remaining - t
        return state.copy(remaining = r, cleared = state.cleared + t,
            hintsRemaining = state.hintsRemaining - 1, isComplete = r.isEmpty(), lastError = null)
    }

    fun escapableArrows(state: PuzzleState): Set<CellKey> =
        state.remaining.keys.filter { canEscape(state, it) }.toSet()

    /** Share of the grid that currently holds an arrow (0f..1f) — used by the
     *  board renderer's ghost layer and by generator regression checks. */
    fun fillRatio(level: Level): Float = level.arrows.size.toFloat() / (level.gridRows * level.gridCols)
}

// ── Level Generator (Most-Constrained-First Density Fill) ───────────────────
//
// Diagnostic-report findings (own app vs competitor "Arrow Puzzle" recordings,
// compared frame-by-frame, Aug 2026):
//  • Old generator picked an explicit target arrow count per level and stopped
//    there once it hit that number (or ran out of safe placements trying) —
//    boards came out mostly empty, and the reported bug (Level 2/3 showing a
//    single lonely arrow on an otherwise blank 4x4 grid) came from exactly
//    that: the placement loop giving up early with almost nothing placed.
//  • Competitor boards tile the grid almost completely (~90-100% of cells) —
//    a dense connected "labyrinth" from the very first frame.
//  • Fix: density is no longer a tuning knob you can fall short of. generate()
//    repeatedly fills whichever empty cell has the FEWEST legal directions
//    left (most-constrained-first) until literally no empty cell can accept
//    an arrow any more. Racing to fill cells that are about to become
//    unplaceable is what pushes fill from ~70% (random order) up to ~95%+.
//  • Solvability is structural, not checked-and-hoped-for: an arrow is only
//    ever placed on a ray that's clear of every arrow placed *before* it, so
//    undoing placements in reverse order is always a valid solve — no
//    backtracking search is needed to prove a board works.
//  • Difficulty now rides on grid size (4x4 tutorial → 10x10 hard) plus how
//    many arrows are already tappable the instant the board loads
//    (maxStartOpen) — a board that's mostly free on load feels pre-solved.
object LevelGenerator {

    /** Generate level for any level number — unlimited levels. */
    fun forLevel(levelNum: Int): Level {
        val p = paramsFor(levelNum)
        var best: List<ArrowDef>? = null
        var bestOpenRatio = Float.MAX_VALUE

        for (attempt in 0 until 30) {
            val seed = levelNum * 104_729L + attempt * 7_919L + 17L
            val arrows = generate(p.rows, p.cols, seed)
            if (arrows.isEmpty()) continue
            val openRatio = startOpenRatio(arrows, p.rows, p.cols)
            if (openRatio <= p.maxStartOpen) {
                return Level(levelNum, p.rows, p.cols, arrows, p.diff, isTutorial = levelNum == 1)
            }
            if (openRatio < bestOpenRatio) { bestOpenRatio = openRatio; best = arrows }
        }

        val fallback = best ?: generate(p.rows, p.cols, levelNum * 997L + 3L)
        return Level(levelNum, p.rows, p.cols, fallback, p.diff, isTutorial = levelNum == 1)
    }

    /** Lightweight difficulty lookup for a level number, without generating its arrows. */
    fun difficultyFor(levelNum: Int): Difficulty = paramsFor(levelNum).diff

    private fun paramsFor(n: Int): LevelParams = when {
        n <= 2  -> LevelParams(4, 4, Difficulty.Tutorial, 0.45f)
        n <= 6  -> LevelParams(5, 5, Difficulty.Easy, 0.34f)
        n <= 12 -> LevelParams(6, 6, Difficulty.Easy, 0.32f)
        n <= 22 -> LevelParams(6, 6, Difficulty.Normal, 0.30f)
        n <= 35 -> LevelParams(7, 7, Difficulty.Normal, 0.28f)
        n <= 55 -> LevelParams(8, 8, Difficulty.Hard, 0.26f)
        n <= 80 -> LevelParams(9, 9, Difficulty.Hard, 0.26f)
        else    -> LevelParams(10, 10, Difficulty.Hard, 0.24f)
    }

    /** Fills the grid most-constrained-cell-first so density lands ~90-100%.
     *  Every placement's ray is checked only against cells placed *before* it,
     *  which is what makes reverse placement order a guaranteed valid solve. */
    private fun generate(rows: Int, cols: Int, seed: Long): List<ArrowDef> {
        val rng = Random(seed)
        val occupied = HashSet<CellKey>(rows * cols)
        val placed = ArrayList<ArrowDef>(rows * cols)
        val allCells = ArrayList<CellKey>(rows * cols).apply {
            for (r in 0 until rows) for (c in 0 until cols) add(CellKey(r, c))
        }

        while (true) {
            var bestCell: CellKey? = null
            var bestFree: List<Direction> = emptyList()
            var bestCount = 5
            for (cell in allCells) {
                if (cell in occupied) continue
                val free = Direction.entries.filter { d -> rayClear(cell, d, occupied, rows, cols) }
                if (free.isEmpty()) continue
                if (free.size < bestCount || (free.size == bestCount && rng.nextFloat() < 0.35f)) {
                    bestCount = free.size; bestCell = cell; bestFree = free
                }
            }
            val cell = bestCell ?: break

            // Prefer the direction whose ray runs closest to existing arrows —
            // keeps the board a tangled knot rather than lines pointing out
            // into empty space at random.
            val dir = bestFree.maxByOrNull { d ->
                rayCells(cell, d, rows, cols).count { rc -> occupied.any { o -> adjacent(o, rc) } }
            } ?: bestFree[rng.nextInt(bestFree.size)]

            placed.add(ArrowDef(cell.row, cell.col, dir))
            occupied.add(cell)
        }
        return placed
    }

    private fun startOpenRatio(arrows: List<ArrowDef>, rows: Int, cols: Int): Float {
        val dirMap = arrows.associate { CellKey(it.row, it.col) to it.direction }
        val open = dirMap.keys.count { isEscapable(it, dirMap, rows, cols) }
        return open.toFloat() / arrows.size
    }

    private fun rayClear(from: CellKey, d: Direction, occupied: Set<CellKey>, rows: Int, cols: Int): Boolean {
        var r = from.row + d.dy; var c = from.col + d.dx
        while (r in 0 until rows && c in 0 until cols) {
            if (CellKey(r, c) in occupied) return false
            r += d.dy; c += d.dx
        }
        return true
    }

    private fun rayCells(from: CellKey, d: Direction, rows: Int, cols: Int): List<CellKey> = buildList {
        var r = from.row + d.dy; var c = from.col + d.dx
        while (r in 0 until rows && c in 0 until cols) { add(CellKey(r, c)); r += d.dy; c += d.dx }
    }

    private fun adjacent(a: CellKey, b: CellKey): Boolean =
        (a.row == b.row && kotlin.math.abs(a.col - b.col) == 1) ||
        (a.col == b.col && kotlin.math.abs(a.row - b.row) == 1)

    private data class LevelParams(val rows: Int, val cols: Int, val diff: Difficulty, val maxStartOpen: Float)
}
