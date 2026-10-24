package com.focusguard.app.ui.battles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.db.entity.ScrollBattleEntity
import com.focusguard.app.data.repository.BrainPalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BattlesViewModel @Inject constructor(
    private val repo: BrainPalRepository
) : ViewModel() {

    val battles: StateFlow<List<ScrollBattleEntity>> = repo.getActiveBattles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myCountToday: StateFlow<Int> = repo.getTodayScrollCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // keep "my" score in every active battle equal to today's real count
        viewModelScope.launch {
            combine(repo.getTodayScrollCount(), repo.getActiveBattles()) { c, b -> c to b }
                .collect { (count, list) ->
                    list.filter { it.myScrollCount != count }.forEach { repo.updateMyCount(it.id, count) }
                }
        }
    }

    fun create(name: String) = viewModelScope.launch { if (name.isNotBlank()) repo.createBattle(name.trim()) }
    fun setOpponent(id: Long, count: Int) = viewModelScope.launch { repo.updateOpponentCount(id, count.coerceAtLeast(0)) }
    fun end(id: Long) = viewModelScope.launch { repo.endBattle(id) }
}
