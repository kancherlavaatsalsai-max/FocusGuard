package com.focusguard.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_guard_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    // ── Keys ──────────────────────────────────────────────────────────
    companion object {
        val KEY_TOTAL_COINS          = intPreferencesKey("total_coins")
        val KEY_CURRENT_STREAK       = intPreferencesKey("current_streak")
        val KEY_LONGEST_STREAK       = intPreferencesKey("longest_streak")
        val KEY_TOTAL_FOCUS_MINUTES  = longPreferencesKey("total_focus_minutes")
        val KEY_TOTAL_SESSIONS       = intPreferencesKey("total_sessions")
        val KEY_DAILY_REEL_LIMIT     = intPreferencesKey("daily_reel_limit")
        val KEY_DAILY_SCREEN_GOAL    = intPreferencesKey("daily_screen_goal_minutes")
        val KEY_STRICT_MODE          = booleanPreferencesKey("strict_mode")
        val KEY_BEDTIME_MODE         = booleanPreferencesKey("bedtime_mode")
        val KEY_BEDTIME_HOUR         = intPreferencesKey("bedtime_hour")
        val KEY_MORNING_LOCK_HOUR    = intPreferencesKey("morning_lock_hour")
        val KEY_LAST_STREAK_DATE     = stringPreferencesKey("last_streak_date")
        val KEY_ACTIVE_SESSION_ID    = longPreferencesKey("active_session_id")
        val KEY_DARK_THEME           = booleanPreferencesKey("dark_theme")
        val KEY_FOCUS_PIN            = stringPreferencesKey("focus_pin")
        val KEY_ONBOARDED            = booleanPreferencesKey("onboarded")
        val KEY_FOCUS_ACTIVE         = booleanPreferencesKey("focus_active")
        val KEY_BLOCK_ONLY_FOCUS     = booleanPreferencesKey("block_only_focus")
    }

    // ── Read flows ────────────────────────────────────────────────────
    val totalCoins: Flow<Int> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_TOTAL_COINS] ?: 0 }

    val currentStreak: Flow<Int> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_CURRENT_STREAK] ?: 0 }

    val longestStreak: Flow<Int> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LONGEST_STREAK] ?: 0 }

    val totalFocusMinutes: Flow<Long> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_TOTAL_FOCUS_MINUTES] ?: 0L }

    val dailyReelLimit: Flow<Int> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DAILY_REEL_LIMIT] ?: 30 }

    val strictMode: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_STRICT_MODE] ?: false }

    val bedtimeMode: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_BEDTIME_MODE] ?: false }

    val bedtimeHour: Flow<Int> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_BEDTIME_HOUR] ?: 22 }

    val morningLockHour: Flow<Int> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_MORNING_LOCK_HOUR] ?: 9 }

    val activeSessionId: Flow<Long> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ACTIVE_SESSION_ID] ?: -1L }

    val darkTheme: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DARK_THEME] ?: true }

    val isOnboarded: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDED] ?: false }

    // ── Write functions ───────────────────────────────────────────────
    suspend fun addCoins(amount: Int) = store.edit { prefs ->
        prefs[KEY_TOTAL_COINS] = (prefs[KEY_TOTAL_COINS] ?: 0) + amount
    }

    suspend fun setStreak(current: Int, longest: Int) = store.edit { prefs ->
        prefs[KEY_CURRENT_STREAK] = current
        prefs[KEY_LONGEST_STREAK] = maxOf(longest, current)
    }

    suspend fun addFocusMinutes(minutes: Long) = store.edit { prefs ->
        prefs[KEY_TOTAL_FOCUS_MINUTES] = (prefs[KEY_TOTAL_FOCUS_MINUTES] ?: 0L) + minutes
    }

    suspend fun setDailyReelLimit(limit: Int) = store.edit {
        it[KEY_DAILY_REEL_LIMIT] = limit
    }

    suspend fun setStrictMode(enabled: Boolean) = store.edit {
        it[KEY_STRICT_MODE] = enabled
    }

    suspend fun setBedtimeMode(enabled: Boolean, hour: Int) = store.edit {
        it[KEY_BEDTIME_MODE] = enabled
        it[KEY_BEDTIME_HOUR] = hour
    }

    suspend fun setMorningLockHour(hour: Int) = store.edit {
        it[KEY_MORNING_LOCK_HOUR] = hour
    }

    suspend fun setActiveSessionId(id: Long) = store.edit {
        it[KEY_ACTIVE_SESSION_ID] = id
    }

    suspend fun setDarkTheme(dark: Boolean) = store.edit {
        it[KEY_DARK_THEME] = dark
    }

    suspend fun setOnboarded(done: Boolean) = store.edit {
        it[KEY_ONBOARDED] = done
    }

    suspend fun setLastStreakDate(date: String) = store.edit {
        it[KEY_LAST_STREAK_DATE] = date
    }

    val lastStreakDate: Flow<String> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_STREAK_DATE] ?: "" }

    // ── Focus-session blocking ────────────────────────────────────────
    val focusActive: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_FOCUS_ACTIVE] ?: false }

    suspend fun setFocusActive(active: Boolean) = store.edit {
        it[KEY_FOCUS_ACTIVE] = active
    }

    /** true = blocked apps are only blocked while a focus session is running */
    val blockOnlyDuringFocus: Flow<Boolean> = store.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_BLOCK_ONLY_FOCUS] ?: true }

    suspend fun setBlockOnlyDuringFocus(enabled: Boolean) = store.edit {
        it[KEY_BLOCK_ONLY_FOCUS] = enabled
    }
}
