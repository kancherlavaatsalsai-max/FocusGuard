package com.focusguard.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val strictMode: Boolean = false,
    val reelLimit: Int = 30,
    val bedtimeMode: Boolean = false,
    val bedtimeHour: Int = 22,
    val morningLockHour: Int = 9,
    val darkTheme: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferences
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPrefs.strictMode,
        userPrefs.dailyReelLimit,
        userPrefs.bedtimeMode,
        userPrefs.bedtimeHour,
        userPrefs.morningLockHour,
        userPrefs.darkTheme
    ) { values ->
        SettingsUiState(
            strictMode      = values[0] as Boolean,
            reelLimit       = values[1] as Int,
            bedtimeMode     = values[2] as Boolean,
            bedtimeHour     = values[3] as Int,
            morningLockHour = values[4] as Int,
            darkTheme       = values[5] as Boolean
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setStrictMode(enabled: Boolean) = viewModelScope.launch {
        userPrefs.setStrictMode(enabled)
    }

    fun setReelLimit(limit: Int) = viewModelScope.launch {
        userPrefs.setDailyReelLimit(limit)
    }

    fun setBedtimeMode(enabled: Boolean) = viewModelScope.launch {
        val hour = uiState.value.bedtimeHour
        userPrefs.setBedtimeMode(enabled, hour)
    }

    fun setBedtimeHour(hour: Int) = viewModelScope.launch {
        userPrefs.setBedtimeMode(uiState.value.bedtimeMode, hour)
    }

    fun setMorningLockHour(hour: Int) = viewModelScope.launch {
        userPrefs.setMorningLockHour(hour)
    }

    fun setDarkTheme(dark: Boolean) = viewModelScope.launch {
        userPrefs.setDarkTheme(dark)
    }
}
