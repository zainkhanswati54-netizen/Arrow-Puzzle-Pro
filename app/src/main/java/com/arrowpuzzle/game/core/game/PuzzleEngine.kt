package com.arrowpuzzle.game.core.game

import androidx.compose.runtime.Immutable
<<<<<<< HEAD
=======
import kotlin.random.Random
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

// ── Direction ────────────────────────────────────────────────────────────────

enum class Direction(val dx: Int, val dy: Int, val degrees: Float) {
    Up(0, -1, -90f),
    Right(1, 0, 0f),
    Down(0, 1, 90f),
<<<<<<< HEAD
    Left(-1, 0, 180f)
=======
    Left(-1, 0, 180f);

    fun rotatedCW(): Direction = when (this) {
        Up -> Right; Right -> Down; Down -> Left; Left -> Up
    }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
}

// ── Level model ─────────────────────────────────────────────────────────────

enum class Difficulty { Tutorial, Easy, Medium, Hard }

@Immutable
<<<<<<< HEAD
data class ArrowDef(val row: Int, val col: Int, val direction: Direction)
=======
data class ArrowDef(val row: Int, val col: Int, val solution: Direction)
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

@Immutable
data class Level(
    val id: Int,
<<<<<<< HEAD
    val gridRows: Int,
    val gridCols: Int,
=======
    val gridSize: Int,
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    val arrows: List<ArrowDef>,
    val difficulty: Difficulty,
    val isTutorial: Boolean = false
)

<<<<<<< HEAD
// ── Cell key ────────────────────────────────────────────────────────────────
=======
// ── Puzzle state ────────────────────────────────────────────────────────────
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

@Immutable
data class CellKey(val row: Int, val col: Int)

<<<<<<< HEAD
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
=======
@Immutable
data class PuzzleState(
    val level: Level,
    val directions: Map<CellKey, Direction>,
    val moveHistory: List<CellKey> = emptyList(),
    val moveCount: Int = 0,
    val lives: Int = 3,
    val hintsRemaining: Int = 3,
    val isComplete: Boolean = false
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
)

// ── Engine ───────────────────────────────────────────────────────────────────

object PuzzleEngine {

<<<<<<< HEAD
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
=======
    /** Create an initial scrambled state for the given level. */
    fun create(level: Level, random: Random = Random): PuzzleState {
        val directions = mutableMapOf<CellKey, Direction>()
        for (def in level.arrows) {
            val key = CellKey(def.row, def.col)
            // Rotate 1-3 times so the arrow is never already correct
            val rotations = random.nextInt(1, 4)
            var dir = def.solution
            repeat(rotations) { dir = dir.rotatedCW() }
            directions[key] = dir
        }
        return PuzzleState(level = level, directions = directions)
    }

    /** Rotate a single cell clockwise and return updated state. */
    fun rotate(state: PuzzleState, cell: CellKey): PuzzleState {
        if (state.isComplete) return state
        val current = state.directions[cell] ?: return state
        val newDir = current.rotatedCW()
        val newDirs = state.directions.toMutableMap().apply { this[cell] = newDir }
        val newHistory = state.moveHistory + cell
        val newCount = state.moveCount + 1
        val complete = checkComplete(state.level, newDirs)
        return state.copy(
            directions = newDirs,
            moveHistory = newHistory,
            moveCount = newCount,
            isComplete = complete
        )
    }

    /** Undo the last move. */
    fun undo(state: PuzzleState): PuzzleState {
        if (state.isComplete || state.moveHistory.isEmpty()) return state
        val lastCell = state.moveHistory.last()
        val current = state.directions[lastCell] ?: return state
        // Rotate CW 3 times = rotate CCW once (undo)
        var dir = current
        repeat(3) { dir = dir.rotatedCW() }
        val newDirs = state.directions.toMutableMap().apply { this[lastCell] = dir }
        return state.copy(
            directions = newDirs,
            moveHistory = state.moveHistory.dropLast(1),
            moveCount = state.moveCount + 1 // still counts as a move
        )
    }

    /** Reveal the correct direction for one random incorrect cell. */
    fun hint(state: PuzzleState): PuzzleState {
        if (state.isComplete || state.hintsRemaining <= 0) return state
        val incorrect = state.level.arrows.filter { def ->
            val key = CellKey(def.row, def.col)
            state.directions[key] != def.solution
        }
        if (incorrect.isEmpty()) return state
        val target = incorrect.random()
        val key = CellKey(target.row, target.col)
        val newDirs = state.directions.toMutableMap().apply { this[key] = target.solution }
        val complete = checkComplete(state.level, newDirs)
        return state.copy(
            directions = newDirs,
            hintsRemaining = state.hintsRemaining - 1,
            isComplete = complete
        )
    }

    /** Shuffle all arrows to new random (non-solved) positions. */
    fun shuffle(state: PuzzleState, random: Random = Random): PuzzleState {
        if (state.isComplete) return state
        val newDirs = mutableMapOf<CellKey, Direction>()
        for (def in state.level.arrows) {
            val key = CellKey(def.row, def.col)
            val rotations = random.nextInt(1, 4)
            var dir = def.solution
            repeat(rotations) { dir = dir.rotatedCW() }
            newDirs[key] = dir
        }
        return state.copy(
            directions = newDirs,
            moveHistory = emptyList(),
            moveCount = state.moveCount + 1
        )
    }

    /** Check if every arrow matches its solution direction. */
    private fun checkComplete(level: Level, directions: Map<CellKey, Direction>): Boolean =
        level.arrows.all { def ->
            directions[CellKey(def.row, def.col)] == def.solution
        }

    /** How many cells are currently correct (for progress feedback). */
    fun correctCount(state: PuzzleState): Int =
        state.level.arrows.count { def ->
            state.directions[CellKey(def.row, def.col)] == def.solution
        }

    /** Check if a specific cell is correct. */
    fun isCellCorrect(state: PuzzleState, cell: CellKey): Boolean {
        val def = state.level.arrows.find { it.row == cell.row && it.col == cell.col }
            ?: return false
        return state.directions[cell] == def.solution
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    }
}

// ── Hand-crafted levels ─────────────────────────────────────────────────────

object Levels {

    val all: List<Level> = listOf(
<<<<<<< HEAD

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
=======
        // Level 1: Tutorial — 3×3, 4 arrows, simple L-shape
        // Path: (1,0)→(1,1)→(0,1)→(0,2)
        Level(
            id = 1, gridSize = 3, difficulty = Difficulty.Tutorial, isTutorial = true,
            arrows = listOf(
                ArrowDef(1, 0, Direction.Right),
                ArrowDef(1, 1, Direction.Up),
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Right)
            )
        ),

        // Level 2: Easy — 3×3, 6 arrows, U-shape
        // Path: (2,0)→(1,0)→(0,0)→(0,1)→(0,2)→(1,2)
        Level(
            id = 2, gridSize = 3, difficulty = Difficulty.Easy,
            arrows = listOf(
                ArrowDef(2, 0, Direction.Up),
                ArrowDef(1, 0, Direction.Up),
                ArrowDef(0, 0, Direction.Right),
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Down),
                ArrowDef(1, 2, Direction.Down)
            )
        ),

        // Level 3: Easy — 4×4, 8 arrows, S-curve
        // Path: (3,0)→(3,1)→(3,2)→(2,2)→(1,2)→(1,1)→(1,0)→(0,0)
        Level(
            id = 3, gridSize = 4, difficulty = Difficulty.Easy,
            arrows = listOf(
                ArrowDef(3, 0, Direction.Right),
                ArrowDef(3, 1, Direction.Right),
                ArrowDef(3, 2, Direction.Up),
                ArrowDef(2, 2, Direction.Up),
                ArrowDef(1, 2, Direction.Left),
                ArrowDef(1, 1, Direction.Left),
                ArrowDef(1, 0, Direction.Up),
                ArrowDef(0, 0, Direction.Up)
            )
        ),

        // Level 4: Medium — 4×4, 10 arrows, spiral
        // Path: (0,0)→(1,0)→(2,0)→(2,1)→(2,2)→(1,2)→(1,3)→(0,3)→(0,2)→(0,1)
        Level(
            id = 4, gridSize = 4, difficulty = Difficulty.Medium,
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
            arrows = listOf(
                ArrowDef(0, 0, Direction.Down),
                ArrowDef(1, 0, Direction.Down),
                ArrowDef(2, 0, Direction.Right),
                ArrowDef(2, 1, Direction.Right),
                ArrowDef(2, 2, Direction.Up),
<<<<<<< HEAD
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
=======
                ArrowDef(1, 2, Direction.Right),
                ArrowDef(1, 3, Direction.Up),
                ArrowDef(0, 3, Direction.Left),
                ArrowDef(0, 2, Direction.Left),
                ArrowDef(0, 1, Direction.Left)
            )
        ),

        // Level 5: Medium — 5×5, 14 arrows, zigzag
        // Path: (4,0)→(4,1)→(4,2)→(3,2)→(3,3)→(3,4)→(2,4)→(2,3)→(2,2)→(2,1)→(1,1)→(0,1)→(0,2)→(0,3)
        Level(
            id = 5, gridSize = 5, difficulty = Difficulty.Medium,
            arrows = listOf(
                ArrowDef(4, 0, Direction.Right),
                ArrowDef(4, 1, Direction.Right),
                ArrowDef(4, 2, Direction.Up),
                ArrowDef(3, 2, Direction.Right),
                ArrowDef(3, 3, Direction.Right),
                ArrowDef(3, 4, Direction.Up),
                ArrowDef(2, 4, Direction.Left),
                ArrowDef(2, 3, Direction.Left),
                ArrowDef(2, 2, Direction.Left),
                ArrowDef(2, 1, Direction.Up),
                ArrowDef(1, 1, Direction.Up),
                ArrowDef(0, 1, Direction.Right),
                ArrowDef(0, 2, Direction.Right),
                ArrowDef(0, 3, Direction.Right)
            )
        ),

        // Level 6: Hard — 6×6, 18 arrows, complex serpentine
        // Path: (5,0)→(5,1)→(5,2)→(4,2)→(3,2)→(3,1)→(3,0)→(2,0)→(1,0)→(1,1)→(1,2)→(1,3)→(1,4)→(2,4)→(3,4)→(3,5)→(2,5)→(1,5)
        Level(
            id = 6, gridSize = 6, difficulty = Difficulty.Hard,
            arrows = listOf(
                ArrowDef(5, 0, Direction.Right),
                ArrowDef(5, 1, Direction.Right),
                ArrowDef(5, 2, Direction.Up),
                ArrowDef(4, 2, Direction.Up),
                ArrowDef(3, 2, Direction.Left),
                ArrowDef(3, 1, Direction.Left),
                ArrowDef(3, 0, Direction.Up),
                ArrowDef(2, 0, Direction.Up),
                ArrowDef(1, 0, Direction.Right),
                ArrowDef(1, 1, Direction.Right),
                ArrowDef(1, 2, Direction.Right),
                ArrowDef(1, 3, Direction.Right),
                ArrowDef(1, 4, Direction.Down),
                ArrowDef(2, 4, Direction.Down),
                ArrowDef(3, 4, Direction.Right),
                ArrowDef(3, 5, Direction.Up),
                ArrowDef(2, 5, Direction.Up),
                ArrowDef(1, 5, Direction.Up)
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
            )
        )
    )

    fun byId(id: Int): Level? = all.find { it.id == id }
}
