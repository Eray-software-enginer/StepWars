package com.example.stepwars2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.DailyQuest
import com.example.stepwars2.data.model.DailyQuestGenerator
import com.example.stepwars2.data.model.QuestType
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.service.StepCounterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuestViewModel(application: Application) : AndroidViewModel(application) {

    private val _quests = MutableStateFlow<List<DailyQuest>>(emptyList())
    val quests: StateFlow<List<DailyQuest>> = _quests.asStateFlow()

    private val _claimMessage = MutableStateFlow<String?>(null)
    val claimMessage: StateFlow<String?> = _claimMessage.asStateFlow()

    private var battleCount = 0
    private var winCount = 0

    private val prefs = application.getSharedPreferences("quest_prefs", android.content.Context.MODE_PRIVATE)
    private val todayString: String
        get() = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        loadQuests()
        observeSteps()
    }

    private fun loadQuests() {
        val baseQuests = DailyQuestGenerator.generateDailyQuests()
        val restoredQuests = baseQuests.map { quest ->
            val isClaimed = prefs.getBoolean("${todayString}_${quest.id}_claimed", false)
            quest.copy(isClaimed = isClaimed)
        }
        _quests.value = restoredQuests
    }

    private fun observeSteps() {
        viewModelScope.launch {
            StepCounterService.steps.collect { steps ->
                updateQuestProgress(QuestType.WALK_STEPS, steps)
            }
        }
    }

    fun onBattlePlayed(won: Boolean) {
        battleCount++
        updateQuestProgress(QuestType.PLAY_BATTLES, battleCount)
        if (won) {
            winCount++
            updateQuestProgress(QuestType.WIN_BATTLES, winCount)
        }
    }

    private fun updateQuestProgress(type: QuestType, value: Int) {
        _quests.value = _quests.value.map { quest ->
            if (quest.type == type && !quest.isClaimed) {
                quest.copy(currentValue = value)
            } else quest
        }
    }

    fun claimReward(questId: String) {
        val quest = _quests.value.find { it.id == questId } ?: return
        if (!quest.isCompleted || quest.isClaimed) return

        // Save to SharedPreferences so it survives ViewModel recreation
        prefs.edit().putBoolean("${todayString}_${questId}_claimed", true).apply()

        // Immediately mark as claimed in UI
        _quests.value = _quests.value.map {
            if (it.id == questId) it.copy(isClaimed = true) else it
        }

        viewModelScope.launch {
            try {
                val currentUser = UserStateManager.currentUser()
                if (currentUser == null) {
                    _claimMessage.value = "Kullanıcı bulunamadı"
                    return@launch
                }

                // Calculate new values
                val updates = mutableMapOf<String, Any>()
                updates["stepGold"] = currentUser.stepGold + quest.rewardGold
                updates["gems"] = currentUser.gems + quest.rewardGems

                val newXp = currentUser.xp + quest.rewardXp
                updates["xp"] = newXp
                val newLevel = 1 + (newXp / 100)
                if (newLevel > currentUser.level) {
                    updates["level"] = newLevel
                }

                // Write to Realtime Database
                UserStateManager.updateUser(updates)

                // Show message
                val rewards = mutableListOf<String>()
                if (quest.rewardGold > 0) rewards.add("+${quest.rewardGold} Altın")
                if (quest.rewardGems > 0) rewards.add("+${quest.rewardGems} Elmas")
                if (quest.rewardXp > 0) rewards.add("+${quest.rewardXp} XP")
                _claimMessage.value = "${quest.title}: ${rewards.joinToString(", ")} 🎉"

            } catch (e: Exception) {
                // Revert claim on failure
                prefs.edit().putBoolean("${todayString}_${questId}_claimed", false).apply()
                _quests.value = _quests.value.map {
                    if (it.id == questId) it.copy(isClaimed = false) else it
                }
                _claimMessage.value = "Ödül alınamadı: ${e.message}"
            }
        }
    }

    fun clearClaimMessage() {
        _claimMessage.value = null
    }
}
