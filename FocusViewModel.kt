package com.focusguard.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.prefs.UserPreferences
import com.focusguard.app.data.repository.SessionRepository
import com.focusguard.app.domain.model.FocusSession
import com.focusguard.app.domain.model.SessionState
import com.focusguard.app.domain.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FocusUiState(
    val session: FocusSession = FocusSession(),
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isBreak: Boolean = false,
    val sessionGoal: String = "",
    val selectedType: SessionType = SessionType.POMODORO,
    val customMinutes: Int = 45,
    val currentPomodoro: Int = 1,
    val totalPomodoros: Int = 4,
    /** non-null = a session just finished; value = coins earned */
    val completedCoins: Int? = null
)

/** Scoped to the Activity (see FocusScreen) so the timer survives leaving the screen. */
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(FocusUiState())
    val state: StateFlow<FocusUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var activeSessionId = -1L
    private var endAtMs = 0L

    fun setGoal(goal: String) = _state.update { it.copy(sessionGoal = goal) }
    fun setType(type: SessionType) = _state.update { it.copy(selectedType = type) }
    fun setCustomMinutes(min: Int) = _state.update { it.copy(customMinutes = min) }

    fun startSession() {
        val st = _state.value
        val durationMin = when (st.selectedType) {
            SessionType.POMODORO  -> 25
            SessionType.DEEP_WORK -> 90
            SessionType.CUSTOM    -> st.customMinutes
        }
        val breakMin = when (st.selectedType) {
            SessionType.POMODORO  -> 5
            SessionType.DEEP_WORK -> 15
            SessionType.CUSTOM    -> 10
        }
        val session = FocusSession(
            goal = st.sessionGoal, type = st.selectedType,
            durationMinutes = durationMin, breakMinutes = breakMin, state = SessionState.RUNNING
        )
        viewModelScope.launch {
            activeSessionId = sessionRepository.startSession(session)
            prefs.setFocusActive(true)
            _state.update {
                it.copy(
                    session = session.copy(id = activeSessionId),
                    remainingSeconds = durationMin * 60,
                    isRunning = true, isPaused = false, isBreak = false,
                    currentPomodoro = 1, completedCoins = null
                )
            }
            startTicker(durationMin * 60)
        }
    }

    fun pauseSession() {
        timerJob?.cancel()
        _state.update { it.copy(isRunning = false, isPaused = true) }
        viewModelScope.launch {
            sessionRepository.pauseSession(activeSessionId)
            prefs.setFocusActive(false)
        }
    }

    fun resumeSession() {
        val st = _state.value
        _state.update { it.copy(isRunning = true, isPaused = false) }
        viewModelScope.launch {
            sessionRepository.resumeSession(activeSessionId)
            prefs.setFocusActive(!st.isBreak)
        }
        startTicker(st.remainingSeconds)
    }

    fun stopSession() {
        timerJob?.cancel()
        viewModelScope.launch {
            sessionRepository.abandonSession(activeSessionId)
            prefs.setFocusActive(false)
            _state.update { FocusUiState(selectedType = it.selectedType, customMinutes = it.customMinutes) }
            activeSessionId = -1L
        }
    }

    /** Skip the current break and go straight back to focus. */
    fun skipBreak() {
        val st = _state.value
        if (!st.isBreak) return
        viewModelScope.launch { prefs.setFocusActive(true) }
        _state.update { it.copy(isBreak = false, remainingSeconds = it.session.durationMinutes * 60) }
        startTicker(st.session.durationMinutes * 60)
    }

    fun dismissComplete() {
        _state.update { FocusUiState(selectedType = it.selectedType, customMinutes = it.customMinutes) }
    }

    // Wall-clock based: stays accurate even if the screen was off.
    private fun startTicker(seconds: Int) {
        timerJob?.cancel()
        endAtMs = System.currentTimeMillis() + seconds * 1000L
        timerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = ((endAtMs - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
                _state.update { it.copy(remainingSeconds = remaining) }
                if (remaining <= 0) break
                delay(250)
            }
            if (isActive) onTimerComplete()
        }
    }

    private suspend fun onTimerComplete() {
        val st = _state.value
        if (!st.isBreak) {
            val morePomodoros = st.selectedType == SessionType.POMODORO && st.currentPomodoro < st.totalPomodoros
            if (morePomodoros) {
                prefs.setFocusActive(false) // apps are allowed during the break
                _state.update {
                    it.copy(isBreak = true, remainingSeconds = it.session.breakMinutes * 60,
                        currentPomodoro = it.currentPomodoro + 1)
                }
                startTicker(st.session.breakMinutes * 60)
            } else {
                val minutes = st.session.durationMinutes * st.currentPomodoro
                val coins = sessionRepository.completeSession(activeSessionId, minutes)
                prefs.setFocusActive(false)
                _state.update {
                    it.copy(isRunning = false, isPaused = false, remainingSeconds = 0, completedCoins = coins)
                }
            }
        } else {
            prefs.setFocusActive(true)
            _state.update { it.copy(isBreak = false, remainingSeconds = it.session.durationMinutes * 60) }
            startTicker(st.session.durationMinutes * 60)
        }
    }
}
