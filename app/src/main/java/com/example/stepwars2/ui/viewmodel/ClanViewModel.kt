package com.example.stepwars2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.Clan
import com.example.stepwars2.data.model.ClanMember
import com.example.stepwars2.data.model.ClanMessage
import com.example.stepwars2.data.repository.ClanRepository
import com.example.stepwars2.data.repository.UserStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ClanViewModel : ViewModel() {
    private val repository = ClanRepository()

    private val _clans = MutableStateFlow<List<Clan>>(emptyList())
    val clans: StateFlow<List<Clan>> = _clans.asStateFlow()

    private val _myClan = MutableStateFlow<Clan?>(null)
    val myClan: StateFlow<Clan?> = _myClan.asStateFlow()

    private val _members = MutableStateFlow<List<ClanMember>>(emptyList())
    val members: StateFlow<List<ClanMember>> = _members.asStateFlow()

    private val _messages = MutableStateFlow<List<ClanMessage>>(emptyList())
    val messages: StateFlow<List<ClanMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0=Klan Bul, 1=Klanım
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            val user = UserStateManager.currentUser()
            if (user != null && user.clanId.isNotEmpty()) {
                _selectedTab.value = 1
                loadMyClan(user.clanId)
            } else {
                _selectedTab.value = 0
            }
            loadClans()
            _isLoading.value = false
        }
    }

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    private suspend fun loadClans() {
        _clans.value = repository.getClans()
    }

    private suspend fun loadMyClan(clanId: String) {
        _myClan.value = repository.getClan(clanId)
        if (_myClan.value != null) {
            // Start listening to members and messages
            viewModelScope.launch {
                repository.listenMembers(clanId).collect {
                    _members.value = it
                }
            }
            viewModelScope.launch {
                repository.listenMessages(clanId).collect {
                    _messages.value = it
                }
            }
        }
    }

    fun createClan(name: String, description: String, badge: String, minTrophies: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createClan(name, description, badge, minTrophies)
            result.fold(
                onSuccess = { clanId ->
                    _message.value = "Klan oluşturuldu! ⚔️"
                    loadMyClan(clanId)
                    _selectedTab.value = 1
                    loadClans()
                },
                onFailure = { e ->
                    _message.value = e.message
                }
            )
            _isLoading.value = false
        }
    }

    fun joinClan(clanId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.joinClan(clanId)
            result.fold(
                onSuccess = {
                    _message.value = "Klana katıldın! 🎉"
                    loadMyClan(clanId)
                    _selectedTab.value = 1
                    loadClans()
                },
                onFailure = { e ->
                    _message.value = e.message
                }
            )
            _isLoading.value = false
        }
    }

    fun leaveClan() {
        val clanId = _myClan.value?.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.leaveClan(clanId)
            result.fold(
                onSuccess = {
                    _message.value = "Klandan ayrıldın."
                    _myClan.value = null
                    _members.value = emptyList()
                    _messages.value = emptyList()
                    _selectedTab.value = 0
                    loadClans()
                },
                onFailure = { e ->
                    _message.value = e.message
                }
            )
            _isLoading.value = false
        }
    }

    fun kickMember(targetUid: String) {
        val clanId = _myClan.value?.id ?: return
        viewModelScope.launch {
            val result = repository.kickMember(clanId, targetUid)
            result.fold(
                onSuccess = { _message.value = "Üye atıldı." },
                onFailure = { e -> _message.value = e.message }
            )
        }
    }

    fun setMemberRole(targetUid: String, newRole: String) {
        val clanId = _myClan.value?.id ?: return
        viewModelScope.launch {
            val result = repository.setMemberRole(clanId, targetUid, newRole)
            result.fold(
                onSuccess = {
                    val roleName = if (newRole == "admin") "Yönetici" else "Üye"
                    _message.value = "Rol güncellendi: $roleName"
                },
                onFailure = { e -> _message.value = e.message }
            )
        }
    }

    fun sendMessage(text: String) {
        val clanId = _myClan.value?.id ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(clanId, text.trim())
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        loadData()
    }
}
