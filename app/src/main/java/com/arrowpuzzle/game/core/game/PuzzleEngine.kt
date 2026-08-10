package com.arrowpuzzle.game.core.game

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.min
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

// ── Level Generator (Reverse Construction + Difficulty Scoring) ─────────────
//
// Research notes (competitor analysis — Arrow Puzzle Escape, Arrow Out, Arrows:
// Puzzle Escape, Playgama Arrow Puzzle, CrazyGames Arrow Escape):
//  • Core rule is universal: tap an arrow, it exits if its ray to the border is
//    empty; a blocked tap costs a life. Our PuzzleEngine already matches this.
//  • What separates a *good* board from a random one is that the puzzle "feels
//    like a knot": most arrows should be blocked at the start, and clearing it
//    should force a fairly narrow, non-obvious order (a few arrows are hard
//    dependencies for many others). A board where 60%+ of arrows are already
//    escapable on load feels broken/trivial — competitors keep that ratio low
//    (roughly 10-25%) even on early levels.
//  • Difficulty should scale on THREE independent axes, not just arrow count:
//      1. Grid size (more real-estate, longer rays)
//      2. Arrow density (how packed the grid is)
//      3. "Knot depth" — how many arrows are mutually blocking in a chain
//         before the board opens up (a solver-derived metric), similar to how
//         Pips/CrazyGames scale from tiny boards up to 10x10+ hard boards.
//  • We now generate against that knot-depth target instead of just checking
//    "is it solvable at all", and reject boards that are technically solvable
//    but start out mostly wide open (too easy) or generate too few real
//    dependencies (feels like arrow count padding rather than a puzzle).
object LevelGenerator {

    /** Generate level for any level number — unlimited levels. */
    fun forLevel(levelNum: Int): Level {
        val (rows, cols, target, diff) = paramsFor(levelNum)
        val minKnotDepth = minKnotDepthFor(levelNum)
        val maxStartOpenRatio = maxStartOpenRatioFor(levelNum)

        var best: List<ArrowDef>? = null
        var bestScore = -1

        for (attempt in 0..259) {
            val seed = levelNum * 10007L + attempt * 31L
            val arrows = generate(rows, cols, target, seed)
            if (arrows.size < target - 2) continue

            val solveInfo = analyze(arrows, rows, cols)
            if (!solveInfo.solvable) continue // must be solvable

            val startOpenRatio = solveInfo.initialEscapable.toFloat() / arrows.size
            val meetsKnot = solveInfo.knotDepth >= minKnotDepth
            val meetsOpen = startOpenRatio <= maxStartOpenRatio

            if (meetsKnot && meetsOpen) return Level(levelNum, rows, cols, arrows, diff, isTutorial = levelNum == 1)

            // Track the closest-to-target candidate in case nothing hits the bar exactly.
            val score = solveInfo.knotDepth * 100 - (startOpenRatio * 100).toInt()
            if (score > bestScore) { bestScore = score; best = arrows }
        }

        val fallback = best ?: generate(rows, cols, min(target, 8), levelNum * 997L)
        return Level(levelNum, rows, cols, fallback, diff, isTutorial = levelNum == 1)
    }

    /** Lightweight difficulty lookup for a level number, without generating its arrows. */
    fun difficultyFor(levelNum: Int): Difficulty = paramsFor(levelNum).diff

    /** Difficulty curve — grid grows from a 4x4 tutorial up to 10x10 hard boards,
     *  echoing the size progression used by the competitor titles above. */
    private fun paramsFor(n: Int): LevelParams {
        return when {
            n <= 2  -> LevelParams(4, 4, 5 + n * 2, Difficulty.Tutorial)    // 7-9 arrows
            n <= 5  -> LevelParams(5, 5, 9 + n, Difficulty.Easy)            // 11-14
            n <= 10 -> LevelParams(5, 5, 13 + n / 2, Difficulty.Easy)       // 15-18
            n <= 20 -> LevelParams(6, 6, 17 + n / 2, Difficulty.Normal)     // 22-27
            n <= 35 -> LevelParams(7, 7, 24 + n / 3, Difficulty.Normal)     // 30-35
            n <= 55 -> LevelParams(8, 8, 30 + n / 4, Difficulty.Hard)       // 38-43
            n <= 80 -> LevelParams(9, 9, 36 + n / 5, Difficulty.Hard)       // 47-52
            else    -> LevelParams(10, 10, 40 + n / 6, Difficulty.Hard)    // 55+
        }
    }

    /** Minimum required chain-of-dependency depth so the board can't be cleared
     *  in a near-arbitrary order. Ramps gently so early levels stay approachable. */
    private fun minKnotDepthFor(n: Int): Int = when {
        n <= 2  -> 1
        n <= 5  -> 2
        n <= 10 -> 3
        n <= 20 -> 4
        n <= 35 -> 5
        n <= 55 -> 6
        else    -> 7
    }

    /** Ceiling on how many arrows may be immediately escapable on load — keeps
     *  boards from feeling "already half solved" the moment they appear. */
    private fun maxStartOpenRatioFor(n: Int): Float = when {
        n <= 2  -> 0.45f
        n <= 10 -> 0.35f
        n <= 20 -> 0.28f
        else    -> 0.22f
    }

    /** Reverse construction: place arrows so each blocks a previous one, biasing
     *  new placements toward the interior so paths cross and form real knots
     *  instead of a loose scatter along the edges. */
    private fun generate(rows: Int, cols: Int, target: Int, seed: Long): List<ArrowDef> {
        val rng = Random(seed)
        val placed = mutableListOf<ArrowDef>()
        val occupied = mutableSetOf<CellKey>()

        // First arrow on edge pointing out.
        val edges = buildList {
            for (r in 0 until rows) for (c in 0 until cols)
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) add(CellKey(r, c))
        }.shuffled(rng)
        val first = edges.first()
        val firstDir = edgeDirs(first, rows, cols).random(rng)
        placed.add(ArrowDef(first.row, first.col, firstDir)); occupied.add(first)

        var safety = 0
        while (placed.size < target && safety < target * 120) {
            safety++
            // Prefer blockers that currently have the shortest remaining free
            // run to their exit — placing on those tightens the knot instead
            // of always extending the loosest, easiest paths.
            val blocker = placed.filter { pathCells(CellKey(it.row, it.col), it.direction, rows, cols)
                .any { pc -> pc !in occupied } }
                .ifEmpty { placed }
                .let { candidates -> candidates[rng.nextInt(candidates.size)] }
            val bCell = CellKey(blocker.row, blocker.col)
            val path = pathCells(bCell, blocker.direction, rows, cols).filter { it !in occupied }
            if (path.isEmpty()) continue
            // Bias toward the nearer half of the path so new arrows land closer
            // to existing ones, increasing local density/crossings.
            val nearHalf = (path.size + 1) / 2
            val idx = if (rng.nextFloat() < 0.7f) rng.nextInt(nearHalf) else rng.nextInt(path.size)
            val nc = path[idx]
            val free = Direction.entries.filter { d -> isFree(nc, d, occupied, rows, cols) }
            if (free.isEmpty()) continue
            if (occupied.size > 2 && occupied.none { adj(it, nc) }) continue
            // Prefer directions whose ray crosses more of the existing occupied
            // footprint (creates cross-dependencies rather than parallel lines).
            val dir = free.maxByOrNull { d -> pathCells(nc, d, rows, cols).count { it in occupied || occupied.any { o -> adj(o, it) } } }
                ?: free[rng.nextInt(free.size)]
            placed.add(ArrowDef(nc.row, nc.col, dir)); occupied.add(nc)
        }
        return placed
    }

    private data class SolveInfo(val solvable: Boolean, val initialEscapable: Int, val knotDepth: Int)

    /** Solves the board and derives two difficulty signals:
     *   - initialEscapable: how many arrows are free on load (lower = tighter opening)
     *   - knotDepth: length of the longest run, from the start of a valid solve
     *     order, during which the number of simultaneously-escapable arrows stays
     *     at or below 2 (i.e. how long the player is forced through a narrow
     *     "only one or two right answers" corridor before the board loosens up). */
    private fun analyze(arrows: List<ArrowDef>, rows: Int, cols: Int): SolveInfo {
        val rem = arrows.associate { CellKey(it.row, it.col) to it.direction }.toMutableMap()
        val initialEscapable = rem.keys.count { isEscapable(it, rem, rows, cols) }
        val order = mutableListOf<Int>() // escapable-count at each step
        val budget = intArrayOf(20_000) // node budget to keep generation fast even on big boards
        val solvable = solveTracking(rem, rows, cols, order, budget)
        if (!solvable) return SolveInfo(false, initialEscapable, 0)
        var depth = 0
        for (count in order) { if (count <= 2) depth++ else break }
        return SolveInfo(true, initialEscapable, depth)
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

    /** Backtracking solver that also records, at each step, how many arrows
     *  were escapable — used to derive knot depth. Prefers taking the
     *  currently-tightest options first so `order` reflects a natural solve. */
    private fun solveTracking(
        rem: MutableMap<CellKey, Direction>, rows: Int, cols: Int,
        order: MutableList<Int>, budget: IntArray
    ): Boolean {
        if (rem.isEmpty()) return true
        if (budget[0]-- <= 0) return false // out of search budget — treat as inconclusive
        val candidates = rem.keys.filter { isEscapable(it, rem, rows, cols) }
        if (candidates.isEmpty()) return false
        order.add(candidates.size)
        for (cell in candidates) {
            val dir = rem.remove(cell)!!
            if (solveTracking(rem, rows, cols, order, budget)) return true
            rem[cell] = dir
        }
        order.removeAt(order.size - 1)
        return false
    }

    private fun edgeDirs(c: CellKey, r: Int, co: Int) = buildList<Direction> {
        if (c.row == 0) add(Direction.Up); if (c.row == r - 1) add(Direction.Down)
        if (c.col == 0) add(Direction.Left); if (c.col == co - 1) add(Direction.Right)
    }
    private fun pathCells(f: CellKey, d: Direction, r: Int, c: Int) = buildList {
        var rr = f.row + d.dy; var cc = f.col + d.dx
        while (rr in 0 until r && cc in 0 until c) { add(CellKey(rr, cc)); rr += d.dy; cc += d.dx }
    }
    private fun isFree(f: CellKey, d: Direction, o: Set<CellKey>, r: Int, c: Int): Boolean {
        var rr = f.row + d.dy; var cc = f.col + d.dx
        while (rr in 0 until r && cc in 0 until c) { if (CellKey(rr, cc) in o) return false; rr += d.dy; cc += d.dx }
        return true
    }
    private fun adj(a: CellKey, b: CellKey) =
        (a.row == b.row && abs(a.col - b.col) == 1) || (a.col == b.col && abs(a.row - b.row) == 1)

    private data class LevelParams(val rows: Int, val cols: Int, val arrows: Int, val diff: Difficulty)
}
