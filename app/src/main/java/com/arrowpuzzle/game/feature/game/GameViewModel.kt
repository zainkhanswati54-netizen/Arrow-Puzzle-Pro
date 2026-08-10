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
import com.arrowpuzzle.game.core.game.Levels
import com.arrowpuzzle.game.core.game.PuzzleEngine
import com.arrowpuzzle.game.core.game.PuzzleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.progressStore by preferencesDataStore(name = "level_progress")

@Immutable
data class GameUiState(
    val puzzle: PuzzleState? = null,
    val escapable: Set<CellKey> = emptySet(),
    val hintCell: CellKey? = null,
    val showWinCelebration: Boolean = false,
    val showGameOver: Boolean = false,
    val tutorialStep: Int = 0 // 0=show instructions, 1=playing
)

class GameViewModel(
    private val context: Context,
    private val levelId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    val highestCompleted: StateFlow<Int> = context.progressStore.data
        .map { prefs -> prefs[intPreferencesKey("highest_completed")] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init { loadLevel(levelId) }

    fun loadLevel(id: Int) {
        val level = Levels.byId(id) ?: return
        val puzzle = PuzzleEngine.create(level)
        _state.value = GameUiState(
            puzzle = puzzle,
            escapable = PuzzleEngine.escapableArrows(puzzle),
            tutorialStep = if (level.isTutorial) 0 else 1
        )
    }

    fun onCellTap(row: Int, col: Int) {
        val current = _state.value.puzzle ?: return
        if (current.isComplete || current.isGameOver) return

        val cell = CellKey(row, col)
        if (cell !in current.remaining) return

        val canEscape = PuzzleEngine.canEscape(current, cell)
        val next = PuzzleEngine.tap(current, cell)

        when {
            next.isComplete -> SoundEngine.playComplete()
            canEscape -> SoundEngine.playCorrect()
            next.isGameOver -> SoundEngine.playError()
            else -> SoundEngine.playError()
        }

        _state.value = _state.value.copy(
            puzzle = next,
            escapable = PuzzleEngine.escapableArrows(next),
            hintCell = null,
            showWinCelebration = next.isComplete,
            showGameOver = next.isGameOver,
            tutorialStep = if (_state.value.tutorialStep == 0) 1 else _state.value.tutorialStep
        )

        if (next.isComplete) persistProgress(next.level.id)
    }

    fun onHint() {
        val current = _state.value.puzzle ?: return
        if (current.hintsRemaining <= 0) { SoundEngine.playError(); return }
        val hintTarget = PuzzleEngine.findHint(current)
        if (hintTarget != null) {
            SoundEngine.playHint()
            _state.value = _state.value.copy(hintCell = hintTarget)
        } else {
            SoundEngine.playError()
        }
    }

    fun dismissTutorial() {
        _state.value = _state.value.copy(tutorialStep = 1)
    }

    fun dismissWin() {
        _state.value = _state.value.copy(showWinCelebration = false)
    }

    fun retry() {
        loadLevel(levelId)
    }

    private fun persistProgress(completedId: Int) {
        viewModelScope.launch {
            context.progressStore.edit { prefs ->
                val key = intPreferencesKey("highest_completed")
                val current = prefs[key] ?: 0
                if (completedId > current) prefs[key] = completedId
            }
        }
    }

    companion object {
        fun factory(context: Context, levelId: Int): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    GameViewModel(context.applicationContext, levelId) as T
            }
    }
}

class ProgressViewModel(context: Context) : ViewModel() {
    val highestCompleted: StateFlow<Int> = context.progressStore.data
        .map { prefs -> prefs[intPreferencesKey("highest_completed")] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProgressViewModel(context.applicationContext) as T
            }
    }
}
