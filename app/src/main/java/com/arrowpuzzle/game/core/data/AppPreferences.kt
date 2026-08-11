package com.arrowpuzzle.game.core.data

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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

private val Context.settingsStore: DataStore<Preferences> by
    preferencesDataStore(name = "arrow_puzzle_settings")

/**
 * Everything the shell needs to decide what to show on launch. Deliberately tiny —
 * game progress will get its own store once the board logic lands.
 */
@Immutable
data class AppSettings(
    val consentAccepted: Boolean = false,
    val dailyIntroSeen: Boolean = false,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val analyticsEnabled: Boolean = true,
    val personalizedAdsEnabled: Boolean = true,
    val loaded: Boolean = false
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val Consent = booleanPreferencesKey("consent_accepted")
        val DailyIntro = booleanPreferencesKey("daily_intro_seen")
        val Sound = booleanPreferencesKey("sound_enabled")
        val Music = booleanPreferencesKey("music_enabled")
        val Haptics = booleanPreferencesKey("haptics_enabled")
        val Analytics = booleanPreferencesKey("analytics_enabled")
        val PersonalizedAds = booleanPreferencesKey("personalized_ads_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            consentAccepted = prefs[Keys.Consent] ?: false,
            dailyIntroSeen = prefs[Keys.DailyIntro] ?: false,
            soundEnabled = prefs[Keys.Sound] ?: true,
            musicEnabled = prefs[Keys.Music] ?: true,
            hapticsEnabled = prefs[Keys.Haptics] ?: true,
            analyticsEnabled = prefs[Keys.Analytics] ?: true,
            personalizedAdsEnabled = prefs[Keys.PersonalizedAds] ?: true,
            loaded = true
        )
    }

    suspend fun setConsentAccepted(value: Boolean) = put(Keys.Consent, value)
    suspend fun setDailyIntroSeen(value: Boolean) = put(Keys.DailyIntro, value)
    suspend fun setSoundEnabled(value: Boolean) = put(Keys.Sound, value)
    suspend fun setMusicEnabled(value: Boolean) = put(Keys.Music, value)
    suspend fun setHapticsEnabled(value: Boolean) = put(Keys.Haptics, value)
    suspend fun setAnalyticsEnabled(value: Boolean) = put(Keys.Analytics, value)
    suspend fun setPersonalizedAdsEnabled(value: Boolean) = put(Keys.PersonalizedAds, value)

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) {
        context.settingsStore.edit { it[key] = value }
    }
}

class AppViewModel(private val preferences: AppPreferences) : ViewModel() {

    val settings: StateFlow<AppSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings()
    )

    fun acceptConsent() = viewModelScope.launch { preferences.setConsentAccepted(true) }
    fun markDailyIntroSeen() = viewModelScope.launch { preferences.setDailyIntroSeen(true) }
    fun setSound(enabled: Boolean) = viewModelScope.launch { preferences.setSoundEnabled(enabled) }
    fun setMusic(enabled: Boolean) = viewModelScope.launch { preferences.setMusicEnabled(enabled) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { preferences.setHapticsEnabled(enabled) }
    fun setAnalytics(enabled: Boolean) = viewModelScope.launch { preferences.setAnalyticsEnabled(enabled) }
    fun setPersonalizedAds(enabled: Boolean) = viewModelScope.launch { preferences.setPersonalizedAdsEnabled(enabled) }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(AppPreferences(context.applicationContext)) as T
            }
    }
}
