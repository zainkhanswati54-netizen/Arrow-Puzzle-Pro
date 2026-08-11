package com.arrowpuzzle.game.core.game

import androidx.compose.runtime.Immutable
import kotlin.random.Random

enum class Direction(val dx: Int, val dy: Int, val degrees: Float) {
    Up(0, -1, -90f), Right(1, 0, 0f), Down(0, 1, 90f), Left(-1, 0, 180f)
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

// ── Engine ───────────────────────────────────────────────────────────────────

object PuzzleEngine {
    fun create(level: Level): PuzzleState =
        PuzzleState(level = level, remaining = level.arrows.associate { CellKey(it.row, it.col) to it.direction })

    fun canEscape(state: PuzzleState, cell: CellKey): Boolean {
        val dir = state.remaining[cell] ?: return false
        var r = cell.row + dir.dy; var c = cell.col + dir.dx
        while (r in 0 until state.level.gridRows && c in 0 until state.level.gridCols) {
            if (CellKey(r, c) in state.remaining) return false
            r += dir.dy; c += dir.dx
        }
        return true
    }

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

    /** How much of the grid is covered by arrows. Used by the density guard. */
    fun fillRatio(level: Level): Float =
        level.arrows.size.toFloat() / (level.gridRows * level.gridCols).coerceAtLeast(1)

    /** Kept for compatibility; the board renderer now builds its own pipe graph. */
    fun adjacencyPairs(level: Level): List<Pair<CellKey, CellKey>> {
        val cells = level.arrows.map { CellKey(it.row, it.col) }.toSet()
        return buildList {
            for (c in cells) {
                val right = CellKey(c.row, c.col + 1); if (right in cells) add(c to right)
                val down = CellKey(c.row + 1, c.col); if (down in cells) add(c to down)
            }
        }
    }
}

// ── Level Generator (Full-Board Tiling + Reverse Construction) ───────────────
//
// Design notes:
//  • Core rule is universal: tap an arrow, it exits if its ray to the border is
//    empty; a blocked tap costs a life. PuzzleEngine already matches this.
//  • What makes competitor boards read as a dense "labyrinth artwork" is not a
//    different rule — it is that ALMOST EVERY CELL HOLDS AN ARROW. A sparse
//    board (45-75% fill) leaves isolated, floating glyphs with no connecting
//    run, which looks broken, especially late in a level when only a few arrows
//    are left. So generation now tiles the grid as close to 100% as the
//    solvability constraint allows (typically 92-100%).
//  • Guaranteed solvable by construction: an arrow is only ever placed on a cell
//    whose ray to the border is clear of every arrow placed BEFORE it. Removing
//    arrows in reverse placement order is therefore always a valid solution.
//  • Greedy is complete for this puzzle: removing an arrow can never make
//    another arrow non-escapable (escape only depends on the ABSENCE of arrows
//    on the ray). So the escapable set only grows, and a simple greedy sweep
//    both verifies solvability and never needs backtracking.
//  • Difficulty now scales on grid size + how tight the opening is
//    (`maxStartOpenRatio`), since density is pinned near maximum for every tier.
object LevelGenerator {

    /** How many candidate boards to try before settling for the best one seen. */
    private const val ATTEMPTS = 30

    /** Generate level for any level number — unlimited levels. */
    fun forLevel(levelNum: Int): Level {
        val p = paramsFor(levelNum)
        val cells = p.rows * p.cols
        val maxOpen = maxStartOpenRatioFor(levelNum)

        var best: List<ArrowDef>? = null
        var bestScore = -Float.MAX_VALUE

        for (attempt in 0 until ATTEMPTS) {
            val seed = levelNum * 10007L + attempt * 31L
            val arrows = generate(p.rows, p.cols, seed)
            if (arrows.isEmpty()) continue

            val info = analyze(arrows, p.rows, p.cols)
            if (!info.solvable) continue // should never happen — defensive only

            val fill = arrows.size.toFloat() / cells
            val openRatio = info.initialEscapable.toFloat() / arrows.size

            if (fill >= p.minFill && openRatio <= maxOpen) {
                return Level(levelNum, p.rows, p.cols, arrows, p.diff, isTutorial = levelNum == 1)
            }

            // Keep the closest-to-target candidate in case nothing clears the bar.
            val score = fill - openRatio * 0.5f
            if (score > bestScore) { bestScore = score; best = arrows }
        }

        // Fallback still tiles the whole grid — never fall back to a sparse board.
        val fallback = best ?: generate(p.rows, p.cols, levelNum * 997L, longRayBias = 1f)
        return Level(levelNum, p.rows, p.cols, fallback, p.diff, isTutorial = levelNum == 1)
    }

    /** Lightweight difficulty lookup for a level number, without generating its arrows. */
    fun difficultyFor(levelNum: Int): Difficulty = paramsFor(levelNum).diff

    /** Difficulty curve — the grid grows from a 4x4 tutorial up to 10x10 hard
     *  boards. Arrow count is no longer a tuning knob: every tier fills its grid,
     *  so the count follows directly from the grid size (16 → 100 arrows). */
    private fun paramsFor(n: Int): LevelParams = when {
        n <= 2  -> LevelParams(4, 4, 0.95f, Difficulty.Tutorial)   // ~16 arrows
        n <= 6  -> LevelParams(5, 5, 0.94f, Difficulty.Easy)       // ~24
        n <= 12 -> LevelParams(6, 6, 0.94f, Difficulty.Easy)       // ~34
        n <= 22 -> LevelParams(6, 6, 0.94f, Difficulty.Normal)     // ~34
        n <= 35 -> LevelParams(7, 7, 0.93f, Difficulty.Normal)     // ~46
        n <= 55 -> LevelParams(8, 8, 0.92f, Difficulty.Hard)       // ~60
        n <= 80 -> LevelParams(9, 9, 0.90f, Difficulty.Hard)       // ~75
        else    -> LevelParams(10, 10, 0.90f, Difficulty.Hard)     // ~92
    }

    /** Ceiling on how many arrows may be immediately escapable on load — keeps
     *  boards from feeling "already half solved" the moment they appear. This is
     *  now the main difficulty knob alongside grid size. */
    private fun maxStartOpenRatioFor(n: Int): Float = when {
        n <= 2  -> 0.45f
        n <= 12 -> 0.32f
        n <= 22 -> 0.30f
        n <= 35 -> 0.28f
        else    -> 0.26f
    }

    /**
     * Tiles the grid with arrows, densest-possible, guaranteed solvable.
     *
     * Each step picks the empty cell with the FEWEST remaining free directions
     * (most-constrained-first) and places an arrow there in a direction whose ray
     * to the border is currently clear. Most-constrained-first is what pushes
     * fill from ~70% (random order) to ~95%: cells that are about to get boxed in
     * are consumed before they become unusable.
     *
     * Direction choice does not affect how many cells can still be filled (that
     * depends only on which cells are occupied), so it is free to serve
     * difficulty: preferring the LONGEST clear ray means the arrow is more likely
     * to be blocked by arrows placed after it, which tightens the opening.
     */
    private fun generate(rows: Int, cols: Int, seed: Long, longRayBias: Float = 0.85f): List<ArrowDef> {
        val rng = Random(seed)
        val occupied = HashSet<CellKey>(rows * cols * 2)
        val placed = ArrayList<ArrowDef>(rows * cols)
        val empty = ArrayList<CellKey>(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) empty.add(CellKey(r, c))

        val tied = ArrayList<CellKey>(rows * cols)
        val free = ArrayList<Direction>(4)

        while (empty.isNotEmpty()) {
            var fewest = Int.MAX_VALUE
            tied.clear()
            for (cell in empty) {
                var count = 0
                for (d in Direction.entries) if (isFree(cell, d, occupied, rows, cols)) count++
                if (count == 0) continue
                if (count < fewest) { fewest = count; tied.clear(); tied.add(cell) }
                else if (count == fewest) tied.add(cell)
            }
            if (tied.isEmpty()) break // every leftover cell is boxed in — done

            val cell = tied[rng.nextInt(tied.size)]
            free.clear()
            for (d in Direction.entries) if (isFree(cell, d, occupied, rows, cols)) free.add(d)
            if (free.isEmpty()) break

            val dir = if (rng.nextFloat() < longRayBias) {
                free.maxByOrNull { rayLength(cell, it, rows, cols) } ?: free[0]
            } else free[rng.nextInt(free.size)]

            placed.add(ArrowDef(cell.row, cell.col, dir))
            occupied.add(cell)
            empty.remove(cell)
        }
        return placed
    }

    private data class SolveInfo(val solvable: Boolean, val initialEscapable: Int, val narrowSteps: Int)

    /**
     * Greedy solve + difficulty signals.
     *  - initialEscapable: how many arrows are free on load (lower = tighter opening)
     *  - narrowSteps: how many opening moves happen while the number of legal
     *    taps stays small, i.e. how long the player is funnelled before the board
     *    loosens up.
     *
     * Greedy needs no backtracking here: clearing an arrow can only ever unblock
     * other arrows, never block them, so the escapable set grows monotonically.
     */
    private fun analyze(arrows: List<ArrowDef>, rows: Int, cols: Int): SolveInfo {
        val rem = arrows.associate { CellKey(it.row, it.col) to it.direction }.toMutableMap()
        val initialEscapable = rem.keys.count { isEscapable(it, rem, rows, cols) }
        val threshold = maxOf(2, arrows.size / 8)
        var narrowSteps = 0
        var stillNarrow = true

        while (rem.isNotEmpty()) {
            val candidates = rem.keys.filter { isEscapable(it, rem, rows, cols) }
            if (candidates.isEmpty()) return SolveInfo(false, initialEscapable, narrowSteps)
            if (stillNarrow && candidates.size <= threshold) narrowSteps++ else stillNarrow = false
            rem.remove(candidates[0])
        }
        return SolveInfo(true, initialEscapable, narrowSteps)
    }

    private fun isEscapable(cell: CellKey, rem: Map<CellKey, Direction>, rows: Int, cols: Int): Boolean {
        val dir = rem[cell] ?: return false
        var r = cell.row + dir.dy; var c = cell.col + dir.dx
        while (r in 0 until rows && c in 0 until cols) {
            if (CellKey(r, c) in rem) return false
            r += dir.dy; c += dir.dx
        }
        return true
    }

    /** Cells between this cell and the border in the given direction. */
    private fun rayLength(f: CellKey, d: Direction, rows: Int, cols: Int): Int = when (d) {
        Direction.Up -> f.row
        Direction.Down -> rows - 1 - f.row
        Direction.Left -> f.col
        Direction.Right -> cols - 1 - f.col
    }

    private fun isFree(f: CellKey, d: Direction, o: Set<CellKey>, r: Int, c: Int): Boolean {
        var rr = f.row + d.dy; var cc = f.col + d.dx
        while (rr in 0 until r && cc in 0 until c) { if (CellKey(rr, cc) in o) return false; rr += d.dy; cc += d.dx }
        return true
    }

    private data class LevelParams(val rows: Int, val cols: Int, val minFill: Float, val diff: Difficulty)
}
