package com.arrowpuzzle.game.feature.game

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.game.CellKey
import com.arrowpuzzle.game.core.game.LevelGenerator
import com.arrowpuzzle.game.core.game.PuzzleEngine
import com.arrowpuzzle.game.core.game.PuzzleState
import com.arrowpuzzle.game.core.motion.Haptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.progressStore by preferencesDataStore(name = "level_progress")
private val KEY_LEVEL = intPreferencesKey("current_level")

@Immutable
data class GameUiState(
    val puzzle: PuzzleState? = null,
    val escapable: Set<CellKey> = emptySet(),
    val hintCell: CellKey? = null,
    val showWinCelebration: Boolean = false,
    val showGameOver: Boolean = false,
    val tutorialStep: Int = 1, // 0=show overlay, 1=playing
    val loading: Boolean = true
)

class GameViewModel(
    private val context: Context,
    startLevel: Int,
    private val isDaily: Boolean = false
) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()
    private var currentLevelNum = startLevel

    init { loadLevel(startLevel) }

    fun loadLevel(num: Int) {
        currentLevelNum = num
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch(Dispatchers.Default) {
            val level = LevelGenerator.forLevel(num)
            val puzzle = PuzzleEngine.create(level)
            _state.value = GameUiState(
                puzzle = puzzle,
                escapable = PuzzleEngine.escapableArrows(puzzle),
                tutorialStep = if (level.isTutorial) 0 else 1,
                loading = false
            )
        }
    }

    fun onCellTap(row: Int, col: Int) {
        val cur = _state.value.puzzle ?: return
        if (cur.isComplete || cur.isGameOver) return
        val cell = CellKey(row, col)
        if (cell !in cur.remaining) return

        val canEsc = PuzzleEngine.canEscape(cur, cell)
        val next = PuzzleEngine.tap(cur, cell)

        when {
            // Level complete keeps its celebratory buzz — that one's earned.
            next.isComplete -> { SoundEngine.playComplete(); Haptics.levelComplete() }
            // A correct tap plays its click but stays silent on the vibrator —
            // buzzing on every single successful tap got noisy/fatiguing over
            // a full board, and the pipe visually flying off already reads as
            // "that worked" without needing a physical pulse too.
            canEsc -> { SoundEngine.playMove() }
            // Wrong/blocked taps are the one case that should be felt, not
            // just heard — a firmer buzz paired with the error sound so a
            // mis-tap is unmistakable even with the phone muted or in a pocket.
            else -> { SoundEngine.playError(); Haptics.tapWrong() }
        }

        _state.value = _state.value.copy(
            puzzle = next, escapable = PuzzleEngine.escapableArrows(next),
            hintCell = null, showWinCelebration = next.isComplete,
            showGameOver = next.isGameOver,
            tutorialStep = if (_state.value.tutorialStep == 0) 1 else _state.value.tutorialStep
        )
        if (next.isComplete && !isDaily) saveProgress(currentLevelNum + 1)
    }

    /**
     * Reveals one hint and spends the charge that unlocked it.
     *
     * Bug fixed here: this used to only check `hintsRemaining > 0` and
     * highlight a cell — it never actually decremented the counter. Once a
     * player watched a single ad, `hintsRemaining` got bumped to 1 and then
     * sat there forever, so every hint after the first came for free with no
     * ad required. Since hints are ad-only (see [PuzzleState.hintsRemaining]),
     * every call here now spends exactly the one charge it was granted.
     */
    fun onHint() {
        val cur = _state.value.puzzle ?: return
        if (cur.hintsRemaining <= 0) { SoundEngine.playError(); Haptics.tapWrong(); return }
        val h = PuzzleEngine.findHint(cur)
        val spent = cur.copy(hintsRemaining = cur.hintsRemaining - 1)
        if (h != null) {
            SoundEngine.playHint(); Haptics.tapButton()
            _state.value = _state.value.copy(puzzle = spent, hintCell = h)
        } else {
            // No escapable arrow exists right now (shouldn't happen on a
            // solvable board, but never eat the player's earned charge on a
            // dead-end lookup).
            SoundEngine.playError(); Haptics.tapWrong()
        }
    }

    /** Grants exactly one hint charge (earned by watching a rewarded ad) and
     *  immediately reveals it. */
    fun grantBonusHint() {
        val cur = _state.value.puzzle ?: return
        _state.value = _state.value.copy(puzzle = cur.copy(hintsRemaining = cur.hintsRemaining + 1))
        onHint()
    }

    fun nextLevel() {
        _state.value = _state.value.copy(showWinCelebration = false)
        loadLevel(currentLevelNum + 1)
    }

    fun retry() { loadLevel(currentLevelNum) }

    fun dismissTutorial() { _state.value = _state.value.copy(tutorialStep = 1) }

    private fun saveProgress(nextLevel: Int) {
        viewModelScope.launch {
            context.progressStore.edit { prefs ->
                val cur = prefs[KEY_LEVEL] ?: 1
                if (nextLevel > cur) prefs[KEY_LEVEL] = nextLevel
            }
        }
    }

    companion object {
        fun factory(ctx: Context, level: Int, isDaily: Boolean = false) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(c: Class<T>) =
                GameViewModel(ctx.applicationContext, level, isDaily) as T
        }

        /** Read saved progress. */
        fun readProgress(ctx: Context) = ctx.progressStore.data.map { it[KEY_LEVEL] ?: 1 }

        /** Wipes campaign progress back to level 1. Used by Settings > Reset progress. */
        suspend fun resetProgress(ctx: Context) {
            ctx.progressStore.edit { prefs -> prefs[KEY_LEVEL] = 1 }
        }
    }
}
