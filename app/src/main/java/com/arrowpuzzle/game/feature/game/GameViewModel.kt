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
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val showWinCelebration: Boolean = false,
    val tutorialStep: Int = 0 // 0=show instructions, 1=playing, 2=done
)

class GameViewModel(
    private val context: Context,
    private val levelId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    /** Highest completed level, observed by level select. */
    val highestCompleted: StateFlow<Int> = context.progressStore.data
        .map { prefs -> prefs[intPreferencesKey("highest_completed")] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadLevel(levelId)
    }

    fun loadLevel(id: Int) {
        val level = Levels.byId(id) ?: return
        val puzzle = PuzzleEngine.create(level)
        _state.value = GameUiState(
            puzzle = puzzle,
            correctCount = PuzzleEngine.correctCount(puzzle),
            totalCount = level.arrows.size,
            tutorialStep = if (level.isTutorial) 0 else 1
        )
    }

    fun onCellTap(row: Int, col: Int) {
        val current = _state.value.puzzle ?: return
        if (current.isComplete) return

        val cell = CellKey(row, col)
        if (cell !in current.directions) return

        val wasBefore = PuzzleEngine.isCellCorrect(current, cell)
        val next = PuzzleEngine.rotate(current, cell)
        val isAfter = PuzzleEngine.isCellCorrect(next, cell)

        // Sound feedback
        when {
            next.isComplete -> SoundEngine.playComplete()
            !wasBefore && isAfter -> SoundEngine.playCorrect()
            else -> SoundEngine.playRotate()
        }

        val correct = PuzzleEngine.correctCount(next)
        _state.value = _state.value.copy(
            puzzle = next,
            correctCount = correct,
            showWinCelebration = next.isComplete,
            tutorialStep = if (_state.value.tutorialStep == 0) 1 else _state.value.tutorialStep
        )

        if (next.isComplete) {
            persistProgress(next.level.id)
        }
    }

    fun onUndo() {
        val current = _state.value.puzzle ?: return
        val next = PuzzleEngine.undo(current)
        if (next !== current) {
            SoundEngine.playButton()
            _state.value = _state.value.copy(
                puzzle = next,
                correctCount = PuzzleEngine.correctCount(next)
            )
        }
    }

    fun onHint() {
        val current = _state.value.puzzle ?: return
        val next = PuzzleEngine.hint(current)
        if (next !== current) {
            SoundEngine.playHint()
            val correct = PuzzleEngine.correctCount(next)
            _state.value = _state.value.copy(
                puzzle = next,
                correctCount = correct,
                showWinCelebration = next.isComplete
            )
            if (next.isComplete) persistProgress(next.level.id)
        } else {
            SoundEngine.playError()
        }
    }

    fun onShuffle() {
        val current = _state.value.puzzle ?: return
        val next = PuzzleEngine.shuffle(current)
        SoundEngine.playButton()
        _state.value = _state.value.copy(
            puzzle = next,
            correctCount = PuzzleEngine.correctCount(next)
        )
    }

    fun dismissTutorial() {
        _state.value = _state.value.copy(tutorialStep = 1)
    }

    fun dismissWin() {
        _state.value = _state.value.copy(showWinCelebration = false)
    }

    private fun persistProgress(completedId: Int) {
        viewModelScope.launch {
            context.progressStore.edit { prefs ->
                val key = intPreferencesKey("highest_completed")
                val current = prefs[key] ?: 0
                if (completedId > current) {
                    prefs[key] = completedId
                }
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

/** Standalone progress reader for level select (no puzzle loaded). */
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
