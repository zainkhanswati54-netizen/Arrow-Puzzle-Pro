package com.arrowpuzzle.game.core.game

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the two properties that make a board look and play like the reference
 * titles: it must tile its grid (dense, connected artwork) and it must always be
 * completable without guessing.
 */
class LevelGeneratorTest {

    private val levelsUnderTest = (1..40) + listOf(45, 55, 60, 70, 80, 90, 100, 120)

    @Test
    fun `boards fill at least 85 percent of the grid`() {
        for (n in levelsUnderTest) {
            val level = LevelGenerator.forLevel(n)
            val fill = PuzzleEngine.fillRatio(level)
            assertTrue(
                "Level $n only fills ${(fill * 100).toInt()}% of its " +
                    "${level.gridRows}x${level.gridCols} grid (${level.arrows.size} arrows)",
                fill >= 0.85f
            )
        }
    }

    @Test
    fun `boards are always solvable`() {
        for (n in levelsUnderTest) {
            val level = LevelGenerator.forLevel(n)
            assertTrue("Level $n cannot be cleared", clearsCompletely(level))
        }
    }

    @Test
    fun `boards never open with more than half the arrows free`() {
        for (n in levelsUnderTest) {
            val level = LevelGenerator.forLevel(n)
            val state = PuzzleEngine.create(level)
            val open = PuzzleEngine.escapableArrows(state).size.toFloat() / level.arrows.size
            assertTrue("Level $n starts ${(open * 100).toInt()}% open — too easy", open <= 0.5f)
        }
    }

    @Test
    fun `a hint is always available on a fresh board`() {
        for (n in levelsUnderTest) {
            val state = PuzzleEngine.create(LevelGenerator.forLevel(n))
            assertNotNull("Level $n has no legal opening move", PuzzleEngine.findHint(state))
        }
    }

    @Test
    fun `no two arrows share a cell`() {
        for (n in levelsUnderTest) {
            val level = LevelGenerator.forLevel(n)
            val cells = level.arrows.map { CellKey(it.row, it.col) }.toSet()
            assertTrue("Level $n has duplicate cells", cells.size == level.arrows.size)
        }
    }

    /**
     * Clearing an arrow can only unblock other arrows, never block them, so a
     * greedy sweep is enough to decide solvability — no backtracking needed.
     */
    private fun clearsCompletely(level: Level): Boolean {
        var state = PuzzleEngine.create(level)
        while (state.remaining.isNotEmpty()) {
            val next = PuzzleEngine.findHint(state) ?: return false
            state = PuzzleEngine.tap(state, next)
        }
        return true
    }
}
