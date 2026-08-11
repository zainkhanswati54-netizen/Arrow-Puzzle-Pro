package com.arrowpuzzle.game.feature.game

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arrowpuzzle.game.BuildConfig
import com.arrowpuzzle.game.core.audio.SoundEngine
import com.arrowpuzzle.game.core.game.CellKey
import com.arrowpuzzle.game.core.game.LevelGenerator
import com.arrowpuzzle.game.core.game.PuzzleEngine
import com.arrowpuzzle.game.core.game.PuzzleState
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

class GameViewModel(private val context: Context, startLevel: Int) : ViewModel() {
    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()
    private var currentLevelNum = startLevel

    init { loadLevel(startLevel) }

    fun loadLevel(num: Int) {
        currentLevelNum = num
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch(Dispatchers.Default) {
            val level = LevelGenerator.forLevel(num)
            // Density regression guard: boards are meant to tile their grid.
            // If the generator ever regresses to a sparse board again, this
            // shows up in Logcat immediately instead of only in a screenshot.
            if (BuildConfig.DEBUG) {
                val fill = PuzzleEngine.fillRatio(level)
                if (fill < MIN_HEALTHY_FILL) {
                    Log.w(
                        "LevelGenerator",
                        "Level $num is sparse: ${level.arrows.size} arrows on " +
                            "${level.gridRows}x${level.gridCols} (${(fill * 100).toInt()}% fill)"
                    )
                }
            }
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
            next.isComplete -> SoundEngine.playComplete()
            canEsc -> SoundEngine.playCorrect()
            else -> SoundEngine.playError()
        }

        _state.value = _state.value.copy(
            puzzle = next, escapable = PuzzleEngine.escapableArrows(next),
            hintCell = null, showWinCelebration = next.isComplete,
            showGameOver = next.isGameOver,
            tutorialStep = if (_state.value.tutorialStep == 0) 1 else _state.value.tutorialStep
        )
        if (next.isComplete) saveProgress(currentLevelNum + 1)
    }

    fun onHint() {
        val cur = _state.value.puzzle ?: return
        if (cur.hintsRemaining <= 0) { SoundEngine.playError(); return }
        val h = PuzzleEngine.findHint(cur)
        if (h != null) { SoundEngine.playHint(); _state.value = _state.value.copy(hintCell = h) }
        else SoundEngine.playError()
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
        /** Below this fill ratio a board starts to look empty/disconnected. */
        private const val MIN_HEALTHY_FILL = 0.85f

        fun factory(ctx: Context, level: Int) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(c: Class<T>) = GameViewModel(ctx.applicationContext, level) as T
        }

        /** Read saved progress. */
        fun readProgress(ctx: Context) = ctx.progressStore.data.map { it[KEY_LEVEL] ?: 1 }
    }
}
