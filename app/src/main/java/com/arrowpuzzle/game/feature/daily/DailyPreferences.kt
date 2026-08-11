package com.arrowpuzzle.game.feature.daily

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private val Context.dailyStore: DataStore<Preferences> by
    preferencesDataStore(name = "arrow_puzzle_daily")

/**
 * One fresh challenge unlocks every 24 hours. Completing it earns that
 * calendar day a permanent star — this is the whole reward loop, so it is
 * backed by a simple set of completed epoch-days rather than a counter,
 * which lets the month grid (and any future "Awards" screen) recompute
 * exactly which days were won without a second source of truth.
 */
class DailyPreferences(private val context: Context) {

    private object Keys {
        val CompletedDays = stringPreferencesKey("completed_epoch_days")
    }

    /** Every calendar day the player has ever completed a daily challenge on. */
    val completedDays: Flow<Set<Long>> = context.dailyStore.data.map { prefs ->
        prefs[Keys.CompletedDays]
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    /** Marks today as done. Safe to call more than once — the set just dedupes. */
    suspend fun markTodayCompleted() {
        val today = LocalDate.now().toEpochDay()
        context.dailyStore.edit { prefs ->
            val current = prefs[Keys.CompletedDays]
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
                ?.toMutableSet()
                ?: mutableSetOf()
            current += today
            prefs[Keys.CompletedDays] = current.joinToString(",")
        }
    }

    /** Clears every completed daily-challenge star. Used by Settings > Reset progress. */
    suspend fun resetProgress() {
        context.dailyStore.edit { prefs -> prefs[Keys.CompletedDays] = "" }
    }
}

data class DailyUiState(
    val completedDays: Set<Long> = emptySet(),
    val loaded: Boolean = false
) {
    val today: LocalDate get() = LocalDate.now()
    val playedToday: Boolean get() = today.toEpochDay() in completedDays

    fun starsIn(month: YearMonth): Int =
        completedDays.count { day ->
            val date = LocalDate.ofEpochDay(day)
            YearMonth.from(date) == month
        }
}

class DailyViewModel(private val preferences: DailyPreferences) : ViewModel() {

    val state: StateFlow<DailyUiState> = preferences.completedDays
        .map { DailyUiState(completedDays = it, loaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyUiState())

    /** Deterministic puzzle id for a given day — same board for everyone on that date. */
    fun levelIdFor(date: LocalDate): Int = DAILY_LEVEL_BASE + (date.toEpochDay() % DAILY_LEVEL_SPAN).toInt()

    fun todaysLevelId(): Int = levelIdFor(LocalDate.now())

    fun onChallengeCompleted() {
        viewModelScope.launch { preferences.markTodayCompleted() }
    }

    companion object {
        /** Pushed well past every campaign level so the two number lines never collide. */
        private const val DAILY_LEVEL_BASE = 100_000
        private const val DAILY_LEVEL_SPAN = 3650

        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DailyViewModel(DailyPreferences(context.applicationContext)) as T
            }
    }
}
