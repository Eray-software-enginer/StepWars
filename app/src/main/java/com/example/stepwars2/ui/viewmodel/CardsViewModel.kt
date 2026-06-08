package com.example.stepwars2.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.Card
import com.example.stepwars2.data.model.UserCard
import com.example.stepwars2.data.repository.AuthRepository
import com.example.stepwars2.data.repository.UserStateManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class CardWithDetails(
    val userCard: UserCard,
    val card: Card
) {
    val effectiveHp: Int get() = card.baseHp + (userCard.level - 1) * 10
    val effectiveAttack: Int get() = card.baseAttack + (userCard.level - 1) * 5
    val effectiveDefense: Int get() = card.baseDefense + (userCard.level - 1) * 3
    val effectiveSpeed: Int get() = card.baseSpeed + (userCard.level - 1) * 1
}

class CardsViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _cards = MutableStateFlow<List<CardWithDetails>>(emptyList())
    val cards: StateFlow<List<CardWithDetails>> = _cards.asStateFlow()

    private val _deckCards = MutableStateFlow<List<CardWithDetails>>(emptyList())
    val deckCards: StateFlow<List<CardWithDetails>> = _deckCards.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _upgradeMessage = MutableStateFlow<String?>(null)
    val upgradeMessage: StateFlow<String?> = _upgradeMessage.asStateFlow()

    // Gold comes from shared UserStateManager
    val userGold: StateFlow<Int> get() {
        val flow = MutableStateFlow(UserStateManager.currentUser()?.stepGold ?: 0)
        viewModelScope.launch {
            UserStateManager.user.collect { user ->
                flow.value = user?.stepGold ?: 0
            }
        }
        return flow.asStateFlow()
    }

    private var cardsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadCards()
    }

    fun loadCards() {
        _isLoading.value = true
        val uid = authRepository.currentUser?.uid ?: run {
            android.util.Log.e("CardsVM", "No authenticated user")
            _isLoading.value = false
            return
        }

        cardsListenerRegistration?.remove()
        
        cardsListenerRegistration = firestore.collection("users")
            .document(uid)
            .collection("cards")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("CardsVM", "Listen failed: ${error.message}", error)
                    loadFallbackCards(uid)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    android.util.Log.d("CardsVM", "Loaded ${snapshot.documents.size} card documents")

                    val allCardMap = Card.allCardsMap()
                    val cardsWithDetails = mutableListOf<CardWithDetails>()

                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val cardId = data["cardId"] as? String ?: doc.id
                        val card = allCardMap[cardId] ?: continue

                        val userCard = UserCard(
                            id = data["id"] as? String ?: doc.id,
                            cardId = cardId,
                            userId = data["userId"] as? String ?: uid,
                            level = (data["level"] as? Number)?.toInt() ?: 1,
                            count = (data["count"] as? Number)?.toInt() ?: 1,
                            inDeck = data["inDeck"] as? Boolean ?: false,
                            deckPosition = (data["deckPosition"] as? Number)?.toInt() ?: -1,
                            energy = (data["energy"] as? Number)?.toInt() ?: 10,
                            maxEnergy = (data["maxEnergy"] as? Number)?.toInt() ?: 10,
                            lastEnergyRefill = (data["lastEnergyRefill"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        cardsWithDetails.add(CardWithDetails(userCard, card))
                    }

                    _cards.value = cardsWithDetails
                    _deckCards.value = cardsWithDetails
                        .filter { it.userCard.inDeck }
                        .sortedBy { it.userCard.deckPosition }

                    android.util.Log.d("CardsVM", "Total: ${cardsWithDetails.size}, Deck: ${_deckCards.value.size}")
                }
                _isLoading.value = false
            }
    }

    private fun loadFallbackCards(uid: String) {
        val starterCards = Card.getStarterCards()
        _cards.value = starterCards.mapIndexed { index, card ->
            CardWithDetails(
                userCard = UserCard(
                    id = card.id,
                    cardId = card.id,
                    userId = uid,
                    level = 1,
                    count = 1,
                    inDeck = true,
                    deckPosition = index
                ),
                card = card
            )
        }
        _deckCards.value = _cards.value
    }

    override fun onCleared() {
        super.onCleared()
        cardsListenerRegistration?.remove()
    }

    fun getUpgradeCost(level: Int): Int {
        return when {
            level < 3 -> 50
            level < 5 -> 100
            level < 7 -> 200
            level < 10 -> 400
            else -> 800
        }
    }

    fun upgradeCard(cardDetail: CardWithDetails) {
        val uid = authRepository.currentUser?.uid ?: return
        val cost = getUpgradeCost(cardDetail.userCard.level)
        val currentGold = UserStateManager.currentUser()?.stepGold ?: 0

        if (currentGold < cost) {
            _upgradeMessage.value = "Yeterli altın yok! (${cost} altın gerekli)"
            return
        }

        if (cardDetail.userCard.level >= 10) {
            _upgradeMessage.value = "Bu kart maksimum seviyede!"
            return
        }

        viewModelScope.launch {
            try {
                val newLevel = cardDetail.userCard.level + 1

                // Update card level in Firestore
                firestore.collection("users")
                    .document(uid)
                    .collection("cards")
                    .document(cardDetail.card.id)
                    .update("level", newLevel)
                    .await()

                // Deduct gold via UserStateManager (Realtime Database)
                UserStateManager.updateUser(mapOf("stepGold" to (currentGold - cost)))

                // Update local state
                _cards.value = _cards.value.map {
                    if (it.card.id == cardDetail.card.id) {
                        it.copy(userCard = it.userCard.copy(level = newLevel))
                    } else it
                }
                _deckCards.value = _cards.value.filter { it.userCard.inDeck }.sortedBy { it.userCard.deckPosition }

                _upgradeMessage.value = "${cardDetail.card.name} Lv.$newLevel oldu! 🎉"
            } catch (e: Exception) {
                _upgradeMessage.value = "Yükseltme başarısız: ${e.message}"
            }
        }
    }

    fun buyEnergy(cardDetail: CardWithDetails) {
        val uid = authRepository.currentUser?.uid ?: return
        val currentGold = UserStateManager.currentUser()?.stepGold ?: 0
        val energyCost = 50
        val energyAmount = 5

        if (currentGold < energyCost) {
            _upgradeMessage.value = "Yeterli altın yok! (${energyCost} altın gerekli)"
            return
        }

        viewModelScope.launch {
            try {
                // Firestore'dan kartın güncel verisini oku
                val docRef = firestore.collection("users")
                    .document(uid).collection("cards")
                    .document(cardDetail.card.id)
                val doc = docRef.get().await()
                val data = doc.data ?: return@launch

                val energy = (data["energy"] as? Number)?.toInt() ?: 10
                val maxEnergy = (data["maxEnergy"] as? Number)?.toInt() ?: 10
                val lastRefill = (data["lastEnergyRefill"] as? Number)?.toLong() ?: System.currentTimeMillis()

                // Zamana göre gerçek enerjiyi hesapla
                val elapsed = System.currentTimeMillis() - lastRefill
                val regenCount = (elapsed / (30 * 60 * 1000)).toInt()
                val currentEnergy = (energy + regenCount).coerceAtMost(maxEnergy)

                if (currentEnergy >= maxEnergy) {
                    _upgradeMessage.value = "Enerji zaten dolu!"
                    return@launch
                }

                val newEnergy = (currentEnergy + energyAmount).coerceAtMost(maxEnergy)
                val newRefill = System.currentTimeMillis()

                // Firestore güncelle
                docRef.update("energy", newEnergy, "lastEnergyRefill", newRefill).await()

                // Altın düş
                UserStateManager.updateUser(mapOf("stepGold" to (currentGold - energyCost)))

                // Lokal state güncelle
                _cards.value = _cards.value.map {
                    if (it.card.id == cardDetail.card.id) {
                        it.copy(userCard = it.userCard.copy(energy = newEnergy, lastEnergyRefill = newRefill))
                    } else it
                }
                _deckCards.value = _cards.value.filter { it.userCard.inDeck }.sortedBy { it.userCard.deckPosition }

                val gained = newEnergy - currentEnergy
                _upgradeMessage.value = "⚡ +$gained Enerji alındı! ($newEnergy/$maxEnergy)"
            } catch (e: Exception) {
                _upgradeMessage.value = "İşlem başarısız: ${e.message}"
            }
        }
    }

    fun clearUpgradeMessage() {
        _upgradeMessage.value = null
    }

    fun refreshCards() {
        loadCards()
    }

    fun getRarityColor(rarity: String): Long {
        return when (rarity) {
            "common" -> 0xFF8B949E
            "rare" -> 0xFF58A6FF
            "epic" -> 0xFFBC8CF2
            "legendary" -> 0xFFFFD700
            else -> 0xFF8B949E
        }
    }

    fun getTypeEmoji(type: String): String {
        return when (type) {
            "tank" -> "🛡️"
            "attacker" -> "⚔️"
            "healer" -> "💚"
            "support" -> "🔮"
            else -> "⚔️"
        }
    }

    /**
     * Kartı desteye ekle veya çıkar.
     */
    fun toggleDeck(cardDetail: CardWithDetails) {
        val uid = authRepository.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                val currentInDeck = cardDetail.userCard.inDeck

                if (currentInDeck) {
                    // Desteden çıkar
                    firestore.collection("users").document(uid)
                        .collection("cards").document(cardDetail.card.id)
                        .update(mapOf("inDeck" to false, "deckPosition" to -1))
                        .await()

                    _cards.value = _cards.value.map {
                        if (it.card.id == cardDetail.card.id) {
                            it.copy(userCard = it.userCard.copy(inDeck = false, deckPosition = -1))
                        } else it
                    }
                } else {
                    // Desteye ekle (maks 4)
                    val currentDeckSize = _cards.value.count { it.userCard.inDeck }
                    if (currentDeckSize >= 4) {
                        _upgradeMessage.value = "Deste dolu! Önce bir kartı çıkar."
                        return@launch
                    }

                    val nextPosition = currentDeckSize
                    firestore.collection("users").document(uid)
                        .collection("cards").document(cardDetail.card.id)
                        .update(mapOf("inDeck" to true, "deckPosition" to nextPosition))
                        .await()

                    _cards.value = _cards.value.map {
                        if (it.card.id == cardDetail.card.id) {
                            it.copy(userCard = it.userCard.copy(inDeck = true, deckPosition = nextPosition))
                        } else it
                    }
                }

                _deckCards.value = _cards.value
                    .filter { it.userCard.inDeck }
                    .sortedBy { it.userCard.deckPosition }

                val action = if (currentInDeck) "desteden çıkarıldı" else "desteye eklendi"
                _upgradeMessage.value = "${cardDetail.card.name} $action!"
            } catch (e: Exception) {
                _upgradeMessage.value = "İşlem başarısız: ${e.message}"
            }
        }
    }
}
