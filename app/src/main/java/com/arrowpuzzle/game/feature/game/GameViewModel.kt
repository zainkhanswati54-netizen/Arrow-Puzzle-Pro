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
<<<<<<< HEAD
    val escapable: Set<CellKey> = emptySet(),
    val hintCell: CellKey? = null,
    val showWinCelebration: Boolean = false,
    val showGameOver: Boolean = false,
    val tutorialStep: Int = 0 // 0=show instructions, 1=playing
=======
    val correctCount: Int = 0,
    val totalCount: Int = 0,
    val showWinCelebration: Boolean = false,
    val tutorialStep: Int = 0 // 0=show instructions, 1=playing, 2=done
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
)

class GameViewModel(
    private val context: Context,
    private val levelId: Int
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

<<<<<<< HEAD
=======
    /** Highest completed level, observed by level select. */
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    val highestCompleted: StateFlow<Int> = context.progressStore.data
        .map { prefs -> prefs[intPreferencesKey("highest_completed")] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

<<<<<<< HEAD
    init { loadLevel(levelId) }
=======
    init {
        loadLevel(levelId)
    }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c

    fun loadLevel(id: Int) {
        val level = Levels.byId(id) ?: return
        val puzzle = PuzzleEngine.create(level)
        _state.value = GameUiState(
            puzzle = puzzle,
<<<<<<< HEAD
            escapable = PuzzleEngine.escapableArrows(puzzle),
=======
            correctCount = PuzzleEngine.correctCount(puzzle),
            totalCount = level.arrows.size,
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
            tutorialStep = if (level.isTutorial) 0 else 1
        )
    }

    fun onCellTap(row: Int, col: Int) {
        val current = _state.value.puzzle ?: return
<<<<<<< HEAD
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
=======
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
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    }

    fun onHint() {
        val current = _state.value.puzzle ?: return
<<<<<<< HEAD
        if (current.hintsRemaining <= 0) { SoundEngine.playError(); return }
        val hintTarget = PuzzleEngine.findHint(current)
        if (hintTarget != null) {
            SoundEngine.playHint()
            _state.value = _state.value.copy(hintCell = hintTarget)
=======
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
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
        } else {
            SoundEngine.playError()
        }
    }

<<<<<<< HEAD
=======
    fun onShuffle() {
        val current = _state.value.puzzle ?: return
        val next = PuzzleEngine.shuffle(current)
        SoundEngine.playButton()
        _state.value = _state.value.copy(
            puzzle = next,
            correctCount = PuzzleEngine.correctCount(next)
        )
    }

>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    fun dismissTutorial() {
        _state.value = _state.value.copy(tutorialStep = 1)
    }

    fun dismissWin() {
        _state.value = _state.value.copy(showWinCelebration = false)
    }

<<<<<<< HEAD
    fun retry() {
        loadLevel(levelId)
    }

=======
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
    private fun persistProgress(completedId: Int) {
        viewModelScope.launch {
            context.progressStore.edit { prefs ->
                val key = intPreferencesKey("highest_completed")
                val current = prefs[key] ?: 0
<<<<<<< HEAD
                if (completedId > current) prefs[key] = completedId
=======
                if (completedId > current) {
                    prefs[key] = completedId
                }
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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

<<<<<<< HEAD
=======
/** Standalone progress reader for level select (no puzzle loaded). */
>>>>>>> e2e958806e5734d2b079726c6ebba9ed15f7b04c
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
