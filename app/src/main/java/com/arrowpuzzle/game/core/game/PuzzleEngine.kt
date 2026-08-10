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

// ── Level Generator (Reverse Construction) ───────────────────────────────────

object LevelGenerator {

    /** Generate level for any level number — unlimited levels. */
    fun forLevel(levelNum: Int): Level {
        val (rows, cols, target, diff) = paramsFor(levelNum)
        // Try seeds until solvable
        for (attempt in 0..199) {
            val seed = levelNum * 10007L + attempt * 31L
            val arrows = generate(rows, cols, target, seed)
            if (arrows.size >= target - 2 && isSolvable(arrows, rows, cols)) {
                return Level(levelNum, rows, cols, arrows, diff, isTutorial = levelNum == 1)
            }
        }
        // Fallback: simpler level
        val arrows = generate(rows, cols, min(target, 8), levelNum * 997L)
        return Level(levelNum, rows, cols, arrows, diff, isTutorial = levelNum == 1)
    }

    /** Difficulty curve matching PDF Section 6. */
    private fun paramsFor(n: Int): LevelParams {
        return when {
            n <= 2  -> LevelParams(4, 4, 4 + n * 2, Difficulty.Tutorial)   // 6-8 arrows
            n <= 5  -> LevelParams(5, 5, 8 + n, Difficulty.Easy)           // 10-13
            n <= 10 -> LevelParams(5, 5, 12 + n, Difficulty.Easy)          // 17-22
            n <= 20 -> LevelParams(6, 6, 16 + n / 2, Difficulty.Normal)    // 21-26
            n <= 40 -> LevelParams(7, 7, 22 + n / 3, Difficulty.Normal)    // 29-35
            n <= 70 -> LevelParams(8, 8, 28 + n / 4, Difficulty.Hard)      // 38-45
            else    -> LevelParams(9, 9, 32 + n / 5, Difficulty.Hard)      // 46+
        }
    }

    /** Reverse construction: place arrows so each blocks a previous one. */
    private fun generate(rows: Int, cols: Int, target: Int, seed: Long): List<ArrowDef> {
        val rng = Random(seed)
        val placed = mutableListOf<ArrowDef>()
        val occupied = mutableSetOf<CellKey>()

        // First arrow on edge pointing out
        val edges = buildList {
            for (r in 0 until rows) for (c in 0 until cols)
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) add(CellKey(r, c))
        }.shuffled(rng)
        val first = edges.first()
        val firstDir = edgeDirs(first, rows, cols).random(rng)
        placed.add(ArrowDef(first.row, first.col, firstDir)); occupied.add(first)

        var safety = 0
        while (placed.size < target && safety < target * 80) {
            safety++
            val blocker = placed[rng.nextInt(placed.size)]
            val bCell = CellKey(blocker.row, blocker.col)
            val path = pathCells(bCell, blocker.direction, rows, cols).filter { it !in occupied }
            if (path.isEmpty()) continue
            val nc = path[rng.nextInt(path.size)]
            val free = Direction.entries.filter { d -> isFree(nc, d, occupied, rows, cols) }
            if (free.isEmpty()) continue
            if (occupied.size > 2 && occupied.none { adj(it, nc) }) continue
            val dir = free[rng.nextInt(free.size)]
            placed.add(ArrowDef(nc.row, nc.col, dir)); occupied.add(nc)
        }
        return placed
    }

    /** Backtracking solver (PDF Section 4). */
    private fun isSolvable(arrows: List<ArrowDef>, rows: Int, cols: Int): Boolean {
        val rem = arrows.associate { CellKey(it.row, it.col) to it.direction }.toMutableMap()
        return solve(rem, rows, cols)
    }

    private fun solve(rem: MutableMap<CellKey, Direction>, rows: Int, cols: Int): Boolean {
        if (rem.isEmpty()) return true
        for (cell in rem.keys.toList()) {
            val dir = rem[cell]!!
            var r = cell.row + dir.dy; var c = cell.col + dir.dx; var ok = true
            while (r in 0 until rows && c in 0 until cols) {
                if (CellKey(r, c) in rem) { ok = false; break }; r += dir.dy; c += dir.dx
            }
            if (!ok) continue
            rem.remove(cell)
            if (solve(rem, rows, cols)) return true
            rem[cell] = dir
        }
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
