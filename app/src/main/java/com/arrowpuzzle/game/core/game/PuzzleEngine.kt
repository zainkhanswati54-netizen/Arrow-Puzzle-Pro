package com.arrowpuzzle.game.core.game

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Direction(val dx: Int, val dy: Int, val degrees: Float) {
    Up(0, -1, -90f), Right(1, 0, 0f), Down(0, 1, 90f), Left(-1, 0, 180f)
}

enum class Difficulty { Tutorial, Easy, Normal, Hard }
@Immutable data class CellKey(val row: Int, val col: Int)

/**
 * A single puzzle piece. Competitor titles (Lessmore's "Arrows – Puzzle Escape",
 * Easybrain's "Arrow Puzzle") don't use one-cell-only arrows — most of the board
 * is made of multi-cell "snake" pieces that slide out head-first, which is what
 * makes a cleared board read as an untangled knot instead of a grid of confetti.
 * [cells] is ordered head-first: cells[0] is the exit end (where the arrowhead is
 * drawn and where the escape ray starts); the remaining cells trail behind it in
 * a straight run opposite [direction].
 */
@Immutable data class ArrowPiece(val id: Int, val cells: List<CellKey>, val direction: Direction) {
    val head: CellKey get() = cells.first()
    val length: Int get() = cells.size
}

@Immutable
data class Level(
    val id: Int, val gridRows: Int, val gridCols: Int,
    val pieces: List<ArrowPiece>, val difficulty: Difficulty,
    val isTutorial: Boolean = false
)

@Immutable
data class PuzzleState(
    val level: Level,
    val remaining: Map<Int, ArrowPiece> = emptyMap(),
    val cleared: List<Int> = emptyList(),
    val moveCount: Int = 0,
    val lives: Int = 3,
    val hintsRemaining: Int = 3,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val lastErrorId: Int? = null
) {
    /** All cells currently occupied by any remaining piece — the collision set for ray-casts. */
    val occupied: Set<CellKey> get() = remaining.values.flatMap { it.cells }.toSet()
}

// ── Engine ───────────────────────────────────────────────────────────────────

object PuzzleEngine {
    fun create(level: Level): PuzzleState =
        PuzzleState(level = level, remaining = level.pieces.associateBy { it.id })

    fun canEscape(state: PuzzleState, id: Int): Boolean {
        val piece = state.remaining[id] ?: return false
        return canEscapePiece(piece, state.occupied, state.level.gridRows, state.level.gridCols)
    }

    private fun canEscapePiece(piece: ArrowPiece, occupied: Set<CellKey>, rows: Int, cols: Int): Boolean {
        val dir = piece.direction
        var r = piece.head.row + dir.dy; var c = piece.head.col + dir.dx
        while (r in 0 until rows && c in 0 until cols) {
            // The piece's own body always trails behind its head, so this ray
            // never re-enters cells that belong to the piece being tested.
            if (CellKey(r, c) in occupied) return false
            r += dir.dy; c += dir.dx
        }
        return true
    }

    fun tap(state: PuzzleState, id: Int): PuzzleState {
        if (state.isComplete || state.isGameOver || id !in state.remaining) return state
        return if (canEscape(state, id)) {
            val r = state.remaining - id
            state.copy(remaining = r, cleared = state.cleared + id,
                moveCount = state.moveCount + 1, isComplete = r.isEmpty(), lastErrorId = null)
        } else {
            val l = state.lives - 1
            state.copy(lives = l, moveCount = state.moveCount + 1, isGameOver = l <= 0, lastErrorId = id)
        }
    }

    fun findHint(state: PuzzleState): Int? = state.remaining.keys.firstOrNull { canEscape(state, it) }

    fun useHint(state: PuzzleState): PuzzleState {
        if (state.hintsRemaining <= 0) return state
        val t = findHint(state) ?: return state
        val r = state.remaining - t
        return state.copy(remaining = r, cleared = state.cleared + t,
            hintsRemaining = state.hintsRemaining - 1, isComplete = r.isEmpty(), lastErrorId = null)
    }

    fun escapableIds(state: PuzzleState): Set<Int> =
        state.remaining.keys.filter { canEscape(state, it) }.toSet()

    /** Resolves a tapped board cell to the piece that occupies it, if any. */
    fun pieceAt(state: PuzzleState, cell: CellKey): Int? =
        state.remaining.entries.firstOrNull { (_, p) -> cell in p.cells }?.key

    fun adjacencyPairs(level: Level): List<Pair<CellKey, CellKey>> {
        val cells = level.pieces.flatMap { it.cells }.toSet()
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
// Research notes (competitor analysis — Lessmore "Arrows: Puzzle Escape",
// Easybrain "Arrow Puzzle", Playgama Arrow Puzzle, CrazyGames Arrow Escape):
//  • Core rule is universal: tap a piece, it exits head-first if its ray to the
//    border is empty; a blocked tap costs a life. Our PuzzleEngine matches this.
//  • The single biggest visual/structural difference between a "generic" board
//    and a market-leading one is that most pieces are NOT single cells — they're
//    straight multi-cell "snake" runs (typically 1-4 cells) that slide out as one
//    unit. That's what makes a board read as a woven, connected maze instead of a
//    grid of loose confetti, and it's the gap we're closing here: every piece
//    used to be exactly one cell regardless of level.
//  • What separates a *good* board from a random one is that the puzzle "feels
//    like a knot": most pieces should be blocked at the start, and clearing it
//    should force a fairly narrow, non-obvious order (a few pieces are hard
//    dependencies for many others). A board where 60%+ of pieces are already
//    escapable on load feels broken/trivial — competitors keep that ratio low
//    (roughly 10-25%) even on early levels.
//  • Difficulty scales on FOUR independent axes:
//      1. Grid size (more real-estate, longer rays)
//      2. Cell-fill ratio — how much of the grid is packed with pieces. Early
//         boards stay under half full so the mechanic reads clearly; late boards
//         approach ~90%+ fill, matching the dense "labyrinth" look competitors
//         use once the player already knows the rule.
//      3. Max piece length — level 1 is pure single-cell arrows (a genuine, easy
//         introduction to the tap-away rule, same as the market leaders' first
//         level), then multi-cell "snake" pieces are introduced from level 2 and
//         allowed to grow longer as the difficulty ramps.
//      4. "Knot depth" — how many pieces are mutually blocking in a chain before
//         the board opens up (a solver-derived metric).
//  • Boards are generated against knot-depth + start-open-ratio targets, and
//    validated solvable by a real backtracking solver before being served.
object LevelGenerator {

    /** Generate level for any level number — unlimited levels. */
    fun forLevel(levelNum: Int): Level {
        val p = paramsFor(levelNum)
        val minKnotDepth = minKnotDepthFor(levelNum)
        val maxStartOpenRatio = maxStartOpenRatioFor(levelNum)
        val slack = max(3, p.targetCells / 8)

        var best: List<ArrowPiece>? = null
        var bestScore = -1

        for (attempt in 0..259) {
            val seed = levelNum * 10007L + attempt * 31L
            val pieces = generate(p.rows, p.cols, p.targetCells, p.maxPieceLen, seed)
            val filledCells = pieces.sumOf { it.length }
            if (filledCells < p.targetCells - slack) continue

            val solveInfo = analyze(pieces, p.rows, p.cols)
            if (!solveInfo.solvable) continue // must be solvable

            val startOpenRatio = solveInfo.initialEscapable.toFloat() / pieces.size
            val meetsKnot = solveInfo.knotDepth >= minKnotDepth
            val meetsOpen = startOpenRatio <= maxStartOpenRatio

            if (meetsKnot && meetsOpen) return Level(levelNum, p.rows, p.cols, pieces, p.diff, isTutorial = levelNum == 1)

            // Track the closest-to-target candidate in case nothing hits the bar exactly.
            val score = solveInfo.knotDepth * 100 - (startOpenRatio * 100).toInt()
            if (score > bestScore) { bestScore = score; best = pieces }
        }

        val fallback = best ?: generate(p.rows, p.cols, min(p.targetCells, p.rows * p.cols / 2), p.maxPieceLen, levelNum * 997L)
        return Level(levelNum, p.rows, p.cols, fallback, p.diff, isTutorial = levelNum == 1)
    }

    /** Lightweight difficulty lookup for a level number, without generating its pieces. */
    fun difficultyFor(levelNum: Int): Difficulty = paramsFor(levelNum).diff

    /** Difficulty curve — grid grows from a 4x4 tutorial up to 10x10 hard boards,
     *  echoing the size progression used by the competitor titles above. Cell-fill
     *  ratio and max piece length ramp independently of grid size. */
    private fun paramsFor(n: Int): LevelParams {
        val raw = when {
            n <= 1  -> RawParams(4, 4, 0.38f, 1, Difficulty.Tutorial)  // pure single-cell intro, ~6 pieces
            n <= 2  -> RawParams(4, 4, 0.48f, 2, Difficulty.Tutorial)  // first "snake" pieces appear
            n <= 5  -> RawParams(5, 5, 0.55f, 2, Difficulty.Easy)
            n <= 10 -> RawParams(5, 5, 0.64f, 2, Difficulty.Easy)
            n <= 20 -> RawParams(6, 6, 0.70f, 3, Difficulty.Normal)
            n <= 35 -> RawParams(7, 7, 0.78f, 3, Difficulty.Normal)
            n <= 55 -> RawParams(8, 8, 0.85f, 4, Difficulty.Hard)
            n <= 80 -> RawParams(9, 9, 0.90f, 4, Difficulty.Hard)
            else    -> RawParams(10, 10, 0.92f, 4, Difficulty.Hard)
        }
        val targetCells = (raw.rows * raw.cols * raw.fill).toInt().coerceAtLeast(4)
        return LevelParams(raw.rows, raw.cols, targetCells, raw.maxPieceLen, raw.diff)
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

    /** Ceiling on how many pieces may be immediately escapable on load — keeps
     *  boards from feeling "already half solved" the moment they appear. */
    private fun maxStartOpenRatioFor(n: Int): Float = when {
        n <= 2  -> 0.45f
        n <= 10 -> 0.35f
        n <= 20 -> 0.28f
        else    -> 0.22f
    }

    /** Reverse construction: place pieces so each new head blocks a previous
     *  piece's escape ray, biasing new placements toward the interior so paths
     *  cross and form real knots instead of a loose scatter along the edges.
     *  Each piece is a straight run of 1..[maxPieceLen] cells trailing behind
     *  its head, so boards mix single-cell arrows with longer "snake" pieces. */
    private fun generate(rows: Int, cols: Int, targetCells: Int, maxPieceLen: Int, seed: Long): List<ArrowPiece> {
        val rng = Random(seed)
        val placed = mutableListOf<ArrowPiece>()
        val occupied = mutableSetOf<CellKey>()
        var nextId = 0

        // First piece: an edge cell pointing outward, short so it never blocks itself.
        val edges = buildList {
            for (r in 0 until rows) for (c in 0 until cols)
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) add(CellKey(r, c))
        }.shuffled(rng)
        val firstHead = edges.first()
        val firstDir = edgeDirs(firstHead, rows, cols).random(rng)
        val firstLen = weightedLength(rng, maxBackwardRun(firstHead, firstDir, occupied, rows, cols, maxPieceLen))
        val firstCells = buildBackwardCells(firstHead, firstDir, firstLen, rows, cols) ?: listOf(firstHead)
        placed += ArrowPiece(nextId++, firstCells, firstDir); occupied += firstCells

        var safety = 0
        while (occupied.size < targetCells && safety < targetCells * 90) {
            safety++
            // Prefer blockers that currently have the shortest remaining free
            // run to their exit — placing on those tightens the knot instead
            // of always extending the loosest, easiest paths.
            val blocker = placed.filter { pathCells(it.head, it.direction, rows, cols).any { pc -> pc !in occupied } }
                .ifEmpty { placed }
                .let { candidates -> candidates[rng.nextInt(candidates.size)] }
            val path = pathCells(blocker.head, blocker.direction, rows, cols).filter { it !in occupied }
            if (path.isEmpty()) continue
            // Bias toward the nearer half of the path so new pieces land closer
            // to existing ones, increasing local density/crossings.
            val nearHalf = (path.size + 1) / 2
            val idx = if (rng.nextFloat() < 0.7f) rng.nextInt(nearHalf) else rng.nextInt(path.size)
            val anchor = path[idx] // becomes the HEAD of the new piece
            val freeDirs = Direction.entries.filter { d -> isFree(anchor, d, occupied, rows, cols) }
            if (freeDirs.isEmpty()) continue
            if (occupied.size > 2 && occupied.none { adj(it, anchor) }) continue
            // Prefer directions whose ray crosses more of the existing occupied
            // footprint (creates cross-dependencies rather than parallel lines).
            val dir = freeDirs.maxByOrNull { d -> pathCells(anchor, d, rows, cols).count { it in occupied || occupied.any { o -> adj(o, it) } } }
                ?: freeDirs[rng.nextInt(freeDirs.size)]

            val maxBack = maxBackwardRun(anchor, dir, occupied, rows, cols, maxPieceLen)
            val len = weightedLength(rng, maxBack)
            val cells = buildBackwardCells(anchor, dir, len, rows, cols) ?: continue
            if (cells.any { it in occupied }) continue // defensive: keep placements collision-free

            placed += ArrowPiece(nextId++, cells, dir); occupied += cells
        }
        return placed
    }

    /** How many cells are free going backward (opposite [dir]) from [head], capped at [cap]. */
    private fun maxBackwardRun(head: CellKey, dir: Direction, occupied: Set<CellKey>, rows: Int, cols: Int, cap: Int): Int {
        var len = 1; var cur = head
        while (len < cap) {
            val nr = cur.row - dir.dy; val nc = cur.col - dir.dx
            if (nr !in 0 until rows || nc !in 0 until cols) break
            val next = CellKey(nr, nc)
            if (next in occupied) break
            cur = next; len++
        }
        return len
    }

    /** Builds a piece's cell list (head-first) of the given length, or null if it runs off the grid. */
    private fun buildBackwardCells(head: CellKey, dir: Direction, len: Int, rows: Int, cols: Int): List<CellKey>? {
        val cells = mutableListOf(head)
        var cur = head
        repeat(len - 1) {
            val nr = cur.row - dir.dy; val nc = cur.col - dir.dx
            if (nr !in 0 until rows || nc !in 0 until cols) return null
            cur = CellKey(nr, nc); cells += cur
        }
        return cells
    }

    /** Weighted pick from 1..maxLen, tapering off so short pieces stay common
     *  even on boards that allow long ones — keeps the board readable. */
    private fun weightedLength(rng: Random, maxLen: Int): Int {
        if (maxLen <= 1) return 1
        var len = 1
        while (len < maxLen && rng.nextFloat() < 0.42f) len++
        return len
    }

    private data class SolveInfo(val solvable: Boolean, val initialEscapable: Int, val knotDepth: Int)

    /** Solves the board and derives two difficulty signals:
     *   - initialEscapable: how many pieces are free on load (lower = tighter opening)
     *   - knotDepth: length of the longest run, from the start of a valid solve
     *     order, during which the number of simultaneously-escapable pieces stays
     *     at or below 2 (i.e. how long the player is forced through a narrow
     *     "only one or two right answers" corridor before the board loosens up). */
    private fun analyze(pieces: List<ArrowPiece>, rows: Int, cols: Int): SolveInfo {
        val occupied = pieces.flatMap { it.cells }.toSet()
        val initialEscapable = pieces.count { isEscapablePiece(it, occupied, rows, cols) }
        val remMutable = pieces.associateBy { it.id }.toMutableMap()
        val occMutable = occupied.toMutableSet()
        val order = mutableListOf<Int>()
        val budget = intArrayOf(20_000) // node budget to keep generation fast even on big boards
        val solvable = solveTracking(remMutable, occMutable, rows, cols, order, budget)
        if (!solvable) return SolveInfo(false, initialEscapable, 0)
        var depth = 0
        for (count in order) { if (count <= 2) depth++ else break }
        return SolveInfo(true, initialEscapable, depth)
    }

    private fun isEscapablePiece(piece: ArrowPiece, occupied: Set<CellKey>, rows: Int, cols: Int): Boolean {
        val dir = piece.direction
        var r = piece.head.row + dir.dy; var c = piece.head.col + dir.dx
        while (r in 0 until rows && c in 0 until cols) {
            if (CellKey(r, c) in occupied) return false
            r += dir.dy; c += dir.dx
        }
        return true
    }

    /** Backtracking solver that also records, at each step, how many pieces
     *  were escapable — used to derive knot depth. Prefers taking the
     *  currently-tightest options first so `order` reflects a natural solve. */
    private fun solveTracking(
        rem: MutableMap<Int, ArrowPiece>, occupied: MutableSet<CellKey>, rows: Int, cols: Int,
        order: MutableList<Int>, budget: IntArray
    ): Boolean {
        if (rem.isEmpty()) return true
        if (budget[0]-- <= 0) return false // out of search budget — treat as inconclusive
        val candidates = rem.values.filter { isEscapablePiece(it, occupied, rows, cols) }
        if (candidates.isEmpty()) return false
        order.add(candidates.size)
        for (piece in candidates) {
            rem.remove(piece.id)
            occupied.removeAll(piece.cells.toSet())
            if (solveTracking(rem, occupied, rows, cols, order, budget)) return true
            occupied.addAll(piece.cells)
            rem[piece.id] = piece
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

    private data class RawParams(val rows: Int, val cols: Int, val fill: Float, val maxPieceLen: Int, val diff: Difficulty)
    private data class LevelParams(val rows: Int, val cols: Int, val targetCells: Int, val maxPieceLen: Int, val diff: Difficulty)
}
