package com.arrowpuzzle.game.core.game

import androidx.compose.runtime.Immutable

// ── Direction ────────────────────────────────────────────────────────────────

enum class Direction(val dx: Int, val dy: Int, val degrees: Float) {
    Up(0, -1, -90f),
    Right(1, 0, 0f),
    Down(0, 1, 90f),
    Left(-1, 0, 180f)
}

// ── Level model ─────────────────────────────────────────────────────────────

enum class Difficulty { Tutorial, Easy, Medium, Hard }

@Immutable
data class ArrowDef(val row: Int, val col: Int, val direction: Direction)

@Immutable
data class Level(
    val id: Int,
    val gridRows: Int,
    val gridCols: Int,
    val arrows: List<ArrowDef>,
    val difficulty: Difficulty,
    val isTutorial: Boolean = false
)

// ── Cell key ────────────────────────────────────────────────────────────────

@Immutable
data class CellKey(val row: Int, val col: Int)

// ── Puzzle state ────────────────────────────────────────────────────────────

@Immutable
data class PuzzleState(
    val level: Level,
    /** Arrows still on the board. */
    val remaining: Map<CellKey, Direction>,
    /** Arrows removed (for trail rendering). */
    val cleared: List<CellKey> = emptyList(),
    val moveCount: Int = 0,
    val lives: Int = 3,
    val hintsRemaining: Int = 3,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false,
    /** Set to the tapped cell when the player makes a wrong move (for error flash). */
    val lastError: CellKey? = null
)

// ── Engine ───────────────────────────────────────────────────────────────────

object PuzzleEngine {

    /** Create initial state with all arrows on the board. */
    fun create(level: Level): PuzzleState {
        val remaining = level.arrows.associate { CellKey(it.row, it.col) to it.direction }
        return PuzzleState(level = level, remaining = remaining)
    }

    /**
     * Check whether an arrow at [cell] has a clear escape path.
     * An arrow can escape if there are NO other remaining arrows
     * between it and the edge of the grid in the direction it points.
     */
    fun canEscape(state: PuzzleState, cell: CellKey): Boolean {
        val dir = state.remaining[cell] ?: return false
        var r = cell.row + dir.dy
        var c = cell.col + dir.dx
        while (r in 0 until state.level.gridRows && c in 0 until state.level.gridCols) {
            if (CellKey(r, c) in state.remaining) return false
            r += dir.dy
            c += dir.dx
        }
        return true
    }

    /**
     * Attempt to tap an arrow at [cell].
     * Returns updated state — either the arrow escapes (success) or a life is lost (blocked).
     */
    fun tap(state: PuzzleState, cell: CellKey): PuzzleState {
        if (state.isComplete || state.isGameOver) return state
        if (cell !in state.remaining) return state

        return if (canEscape(state, cell)) {
            val newRemaining = state.remaining - cell
            val newCleared = state.cleared + cell
            val complete = newRemaining.isEmpty()
            state.copy(
                remaining = newRemaining,
                cleared = newCleared,
                moveCount = state.moveCount + 1,
                isComplete = complete,
                lastError = null
            )
        } else {
            val newLives = state.lives - 1
            state.copy(
                lives = newLives,
                moveCount = state.moveCount + 1,
                isGameOver = newLives <= 0,
                lastError = cell
            )
        }
    }

    /** Find a random arrow that can currently escape (for hints). */
    fun findHint(state: PuzzleState): CellKey? {
        return state.remaining.keys.firstOrNull { canEscape(state, it) }
    }

    /** Use a hint: highlight and auto-clear one escapable arrow. */
    fun useHint(state: PuzzleState): PuzzleState {
        if (state.hintsRemaining <= 0) return state
        val target = findHint(state) ?: return state
        val newRemaining = state.remaining - target
        val newCleared = state.cleared + target
        return state.copy(
            remaining = newRemaining,
            cleared = newCleared,
            hintsRemaining = state.hintsRemaining - 1,
            isComplete = newRemaining.isEmpty(),
            lastError = null
        )
    }

    /** Get all arrows that can currently escape (for visual feedback). */
    fun escapableArrows(state: PuzzleState): Set<CellKey> {
        return state.remaining.keys.filter { canEscape(state, it) }.toSet()
    }

    /** Check if two cells are adjacent (for drawing connecting lines). */
    fun areAdjacent(a: CellKey, b: CellKey): Boolean {
        return (a.row == b.row && kotlin.math.abs(a.col - b.col) == 1) ||
               (a.col == b.col && kotlin.math.abs(a.row - b.row) == 1)
    }

    /** Get all pairs of adjacent cells in the original level layout (for maze lines). */
    fun adjacencyPairs(level: Level): List<Pair<CellKey, CellKey>> {
        val cells = level.arrows.map { CellKey(it.row, it.col) }.toSet()
        val pairs = mutableListOf<Pair<CellKey, CellKey>>()
        for (cell in cells) {
            // Only check right and down to avoid duplicates
            val right = CellKey(cell.row, cell.col + 1)
            if (right in cells) pairs.add(cell to right)
            val down = CellKey(cell.row + 1, cell.col)
            if (down in cells) pairs.add(cell to down)
        }
        return pairs
    }
}

// ── Hand-crafted levels ─────────────────────────────────────────────────────

object Levels {

    val all: List<Level> = listOf(

        // Level 1: Tutorial — 4 arrows, L-shape
        // Clearing order: (1,2)→(1,1)→(1,0)→(0,0)
        Level(
            id = 1, gridRows = 3, gridCols = 3,
            difficulty = Difficulty.Tutorial, isTutorial = true,
            arrows = listOf(
                ArrowDef(0, 0, Direction.Down),
                ArrowDef(1, 0, Direction.Right),
                ArrowDef(1, 1, Direction.Right),
                ArrowDef(1, 2, Direction.Up)
            )
        ),

        // Level 2: Easy — 6 arrows, U-shape
        // Clearing order: (1,2)→(2,2)→(2,1)→(2,0)→(1,0)→(0,0)
        Level(
            id = 2, gridRows = 3, gridCols = 3,
            difficulty = Difficulty.Easy,
            arrows = listOf(
                ArrowDef(0, 0, Direction.Down),
                ArrowDef(1, 0, Direction.Down),
                ArrowDef(2, 0, Direction.Right),
                ArrowDef(2, 1, Direction.Right),
                ArrowDef(2, 2, Direction.Up),
                ArrowDef(1, 2, Direction.Up)
            )
        ),

        // Level 3: Easy — 8 arrows, hook/spiral
        // Clearing order: (3,1)→(2,1)→(2,2)→(2,3)→(1,3)→(0,3)→(0,2)→(0,1)
        Level(
            id = 3, gridRows = 4, gridCols = 4,
            difficulty = Difficulty.Easy,
            arrows = listOf(
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Right),
                ArrowDef(0, 3, Direction.Down),
                ArrowDef(1, 3, Direction.Down),
                ArrowDef(2, 3, Direction.Left),
                ArrowDef(2, 2, Direction.Left),
                ArrowDef(2, 1, Direction.Down),
                ArrowDef(3, 1, Direction.Left)
            )
        ),

        // Level 4: Medium — 12 arrows, S-curve
        // Single clearing path from (4,2) backward
        Level(
            id = 4, gridRows = 5, gridCols = 5,
            difficulty = Difficulty.Medium,
            arrows = listOf(
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Right),
                ArrowDef(0, 3, Direction.Down),
                ArrowDef(1, 3, Direction.Down),
                ArrowDef(2, 3, Direction.Left),
                ArrowDef(2, 2, Direction.Left),
                ArrowDef(2, 1, Direction.Left),
                ArrowDef(2, 0, Direction.Down),
                ArrowDef(3, 0, Direction.Down),
                ArrowDef(4, 0, Direction.Right),
                ArrowDef(4, 1, Direction.Right),
                ArrowDef(4, 2, Direction.Right)
            )
        ),

        // Level 5: Medium — 16 arrows, large S-shape
        // Two starting points: (2,1) left-edge and (5,4) right-edge
        Level(
            id = 5, gridRows = 6, gridCols = 6,
            difficulty = Difficulty.Medium,
            arrows = listOf(
                ArrowDef(0, 0, Direction.Right),
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Right),
                ArrowDef(0, 3, Direction.Right),
                ArrowDef(0, 4, Direction.Down),
                ArrowDef(1, 4, Direction.Down),
                ArrowDef(2, 1, Direction.Left),
                ArrowDef(2, 2, Direction.Left),
                ArrowDef(2, 3, Direction.Left),
                ArrowDef(2, 4, Direction.Left),
                ArrowDef(3, 1, Direction.Down),
                ArrowDef(4, 1, Direction.Right),
                ArrowDef(4, 2, Direction.Right),
                ArrowDef(4, 3, Direction.Right),
                ArrowDef(4, 4, Direction.Down),
                ArrowDef(5, 4, Direction.Right)
            )
        ),

        // Level 6: Hard — 22 arrows, full zigzag
        // Two starting points, long chains
        Level(
            id = 6, gridRows = 6, gridCols = 6,
            difficulty = Difficulty.Hard,
            arrows = listOf(
                ArrowDef(0, 0, Direction.Right),
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Right),
                ArrowDef(0, 3, Direction.Right),
                ArrowDef(0, 4, Direction.Right),
                ArrowDef(0, 5, Direction.Down),
                ArrowDef(1, 5, Direction.Down),
                ArrowDef(2, 0, Direction.Left),
                ArrowDef(2, 1, Direction.Left),
                ArrowDef(2, 2, Direction.Left),
                ArrowDef(2, 3, Direction.Left),
                ArrowDef(2, 4, Direction.Left),
                ArrowDef(2, 5, Direction.Left),
                ArrowDef(3, 0, Direction.Down),
                ArrowDef(4, 0, Direction.Right),
                ArrowDef(4, 1, Direction.Right),
                ArrowDef(4, 2, Direction.Right),
                ArrowDef(4, 3, Direction.Right),
                ArrowDef(4, 4, Direction.Right),
                ArrowDef(4, 5, Direction.Down),
                ArrowDef(5, 4, Direction.Left),
                ArrowDef(5, 5, Direction.Left)
            )
        )
    )

    fun byId(id: Int): Level? = all.find { it.id == id }
}
