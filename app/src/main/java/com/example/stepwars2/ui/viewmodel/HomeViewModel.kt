package com.example.stepwars2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.User
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.service.StepCounterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Shared user state — all screens observe the same data
    val user: StateFlow<User?> = UserStateManager.user

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val dailyGoal = 10000
    val stepsPerGold = 100

    private var lastSyncedSteps = 0

    init {
        // Start listening to Realtime Database
        UserStateManager.startListening()
        observeUserLoaded()
        observeSteps()
    }

    private fun observeUserLoaded() {
        viewModelScope.launch {
            UserStateManager.isLoaded.collect { loaded ->
                _isLoading.value = !loaded
            }
        }
    }

    private fun observeSteps() {
        viewModelScope.launch {
            StepCounterService.steps.collect { steps ->
                _todaySteps.value = steps

                // Sync to Realtime Database every 50 steps
                if (steps > 0 && steps - lastSyncedSteps >= 50) {
                    syncStepsToDatabase(steps)
                    lastSyncedSteps = steps
                }
            }
        }
    }

    private fun syncStepsToDatabase(steps: Int) {
        viewModelScope.launch {
            val currentUser = UserStateManager.currentUser() ?: return@launch
            val stepDiff = (steps - currentUser.todaySteps).coerceAtLeast(0)

            try {
                val updates = mapOf(
                    "todaySteps" to steps,
                    "unconvertedSteps" to (currentUser.unconvertedSteps + stepDiff),
                    "totalSteps" to (currentUser.totalSteps + stepDiff),
                    "lastActive" to System.currentTimeMillis()
                )
                UserStateManager.updateUser(updates)
            } catch (_: Exception) {}
        }
    }

    fun convertStepsToGold() {
        viewModelScope.launch {
            val currentUser = UserStateManager.currentUser() ?: return@launch
            val unconverted = currentUser.unconvertedSteps

            if (unconverted < stepsPerGold) {
                _message.value = "En az $stepsPerGold adım gerekli! (Mevcut: $unconverted)"
                return@launch
            }

            val goldEarned = unconverted / stepsPerGold
            val remainingSteps = unconverted % stepsPerGold

            try {
                UserStateManager.updateUser(mapOf(
                    "stepGold" to (currentUser.stepGold + goldEarned),
                    "unconvertedSteps" to remainingSteps
                ))
                _message.value = "$unconverted adım → $goldEarned altın kazandın! ⭐"
            } catch (_: Exception) {
                _message.value = "Dönüştürme başarısız oldu"
            }
        }
    }

    fun startStepCounter() {
        StepCounterService.start(getApplication())
    }

    fun refreshProfile() {
        // No need to refetch — Realtime Database listener auto-updates
    }

    fun clearMessage() {
        _message.value = null
    }

    fun getLeagueName(league: String): String {
        return when (league) {
            "bronze" -> "Bronz"
            "silver" -> "Gümüş"
            "gold" -> "Altın"
            "platinum" -> "Platin"
            "diamond" -> "Elmas"
            "legendary" -> "Efsanevi"
            "champion" -> "Şampiyon"
            else -> "Bronz"
        }
    }
}
