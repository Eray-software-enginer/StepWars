package com.example.stepwars2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.User
import com.example.stepwars2.data.repository.AuthRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LeaderboardEntry(
    val rank: Int,
    val user: User,
    val isCurrentUser: Boolean
)

class LeaderboardViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance("https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app")
    private val authRepository = AuthRepository()

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentUserRank = MutableStateFlow<LeaderboardEntry?>(null)
    val currentUserRank: StateFlow<LeaderboardEntry?> = _currentUserRank.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0=Kupa, 1=Adım, 2=Seviye
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUid = authRepository.currentUser?.uid

            val sortField = when (_selectedTab.value) {
                0 -> "trophies"
                1 -> "totalSteps"
                2 -> "level"
                else -> "trophies"
            }

            try {
                // Realtime Database queries ascending. We take the last 50 and reverse.
                val snapshot = database.reference.child("users")
                    .orderByChild(sortField)
                    .limitToLast(50)
                    .get()
                    .await()

                val users = mutableListOf<User>()
                for (child in snapshot.children) {
                    try {
                        val user = User(
                            uid = child.child("uid").getValue(String::class.java) ?: "",
                            username = child.child("username").getValue(String::class.java) ?: "",
                            email = child.child("email").getValue(String::class.java) ?: "",
                            avatarUrl = child.child("avatarUrl").getValue(String::class.java) ?: "",
                            level = child.child("level").getValue(Int::class.java) ?: 1,
                            xp = child.child("xp").getValue(Int::class.java) ?: 0,
                            stepGold = child.child("stepGold").getValue(Int::class.java) ?: 0,
                            gems = child.child("gems").getValue(Int::class.java) ?: 0,
                            totalSteps = child.child("totalSteps").getValue(Long::class.java) ?: 0L,
                            todaySteps = child.child("todaySteps").getValue(Int::class.java) ?: 0,
                            trophies = child.child("trophies").getValue(Int::class.java) ?: 0,
                            league = child.child("league").getValue(String::class.java) ?: "bronze"
                        )
                        if (user.uid.isNotEmpty()) users.add(user)
                    } catch (e: Exception) {
                        // ignore malformed nodes
                    }
                }

                // Reverse to make it descending
                users.reverse()

                val entries = users.mapIndexed { index, user ->
                    LeaderboardEntry(
                        rank = index + 1,
                        user = user,
                        isCurrentUser = user.uid == currentUid
                    )
                }

                _entries.value = entries
                _currentUserRank.value = entries.find { it.isCurrentUser }
            } catch (_: Exception) {
                // Offline or error - show empty
                _entries.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun refresh() {
        loadLeaderboard()
    }
}
