package com.example.stepwars2.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepwars2.data.model.Card
import com.example.stepwars2.data.model.UserCard
import com.example.stepwars2.data.model.UserChest
import com.example.stepwars2.data.repository.AuthRepository
import com.example.stepwars2.data.repository.UserRepository
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.data.service.MatchmakingService
import com.example.stepwars2.data.service.OnlineBattleService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

data class BattleCard(
    val name: String,
    val emoji: String,
    val type: String,
    val maxHp: Int,
    val currentHp: Int,
    val attack: Int,
    val defense: Int,
    val speed: Int,
    val level: Int
)

data class BattleLog(
    val message: String,
    val isPlayerAction: Boolean
)

sealed class BattleState {
    object Idle : BattleState()
    data class Searching(val secondsRemaining: Int = 60) : BattleState()
    data class InBattle(
        val playerCards: List<BattleCard>,
        val enemyCards: List<BattleCard>,
        val playerActiveIndex: Int = 0,
        val enemyActiveIndex: Int = 0,
        val isPlayerTurn: Boolean = true,
        val battleLog: List<BattleLog> = emptyList(),
        val enemyName: String = "Rakip",
        val isOnline: Boolean = false,
        val battleId: String = "",
        val isPlayer1: Boolean = true,
        val turnTimeRemaining: Int = TURN_TIME_LIMIT // Sıra zamanlayıcı
    ) : BattleState()
    data class Finished(
        val won: Boolean,
        val trophyChange: Int,
        val goldReward: Int,
        val enemyName: String,
        val isOnline: Boolean = false
    ) : BattleState()
    data class InsufficientGold(val required: Int, val current: Int) : BattleState()
    data class NoEnergy(val cardName: String) : BattleState()
}

const val TURN_TIME_LIMIT = 15 // saniye
const val BATTLE_ENTRY_FEE = 20 // altın giriş ücreti
const val BATTLE_WINNER_PRIZE = 35 // kazanan aldığı toplam altın

class BattleViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val firestore = FirebaseFirestore.getInstance()

    private val _battleState = MutableStateFlow<BattleState>(BattleState.Idle)
    val battleState: StateFlow<BattleState> = _battleState.asStateFlow()

    private var turnTimerJob: Job? = null

    private val aiNames = listOf(
        "Gölge Savaşçı", "Yıldırım Kurt", "Buz Kraliçesi",
        "Ateş Lordu", "Rüzgar Okçusu", "Demir Kalkan",
        "Karanlık Büyücü", "Işık Şövalyesi", "Fırtına Avcısı"
    )

    init {
        // Matchmaking state dinle
        viewModelScope.launch {
            MatchmakingService.state.collect { matchState ->
                when (matchState) {
                    is MatchmakingService.MatchState.Searching -> {
                        _battleState.value = BattleState.Searching(matchState.secondsRemaining)
                    }
                    is MatchmakingService.MatchState.Found -> {
                        startOnlineBattle(matchState.battleId, matchState.isPlayer1)
                    }
                    is MatchmakingService.MatchState.Timeout -> {
                        startBotBattle()
                    }
                    is MatchmakingService.MatchState.Error -> {
                        Log.e("BattleVM", "Matchmaking error: ${matchState.message}")
                        // Hata durumunda hemen bot başlatma, timeout bekle
                    }
                    else -> { }
                }
            }
        }

        // Online battle state dinle
        viewModelScope.launch {
            OnlineBattleService.battleState.collect { onlineState ->
                if (onlineState == null) return@collect
                val currentState = _battleState.value
                if (currentState !is BattleState.InBattle || !currentState.isOnline) return@collect

                val myCards = onlineState.myCards.map { card ->
                    BattleCard(card.name, card.emoji, card.type,
                        card.maxHp, card.currentHp, card.attack,
                        card.defense, card.speed, card.level)
                }
                val opCards = onlineState.opponentCards.map { card ->
                    BattleCard(card.name, card.emoji, card.type,
                        card.maxHp, card.currentHp, card.attack,
                        card.defense, card.speed, card.level)
                }

                val log = currentState.battleLog.toMutableList()
                if (onlineState.lastDamage > 0 && onlineState.lastActionBy.isNotEmpty()) {
                    val myKey = if (onlineState.isPlayer1) "player1" else "player2"
                    val isMyAction = onlineState.lastActionBy == myKey
                    val attackerCards = if (isMyAction) myCards else opCards
                    val attackerIdx = if (isMyAction) onlineState.myActiveIndex else onlineState.opponentActiveIndex

                    val lastMsg = if (log.isNotEmpty()) log.last().message else ""
                    val newMsg = "${attackerCards.getOrNull(attackerIdx)?.emoji ?: "⚔️"} ${attackerCards.getOrNull(attackerIdx)?.name ?: "?"} → ${onlineState.lastDamage} hasar!"
                    if (lastMsg != newMsg) {
                        log.add(BattleLog(newMsg, isMyAction))

                        opCards.forEachIndexed { idx, card ->
                            if (card.currentHp <= 0 && currentState.enemyCards.getOrNull(idx)?.currentHp?.let { it > 0 } == true) {
                                log.add(BattleLog("💀 ${card.name} yenildi!", true))
                            }
                        }
                        myCards.forEachIndexed { idx, card ->
                            if (card.currentHp <= 0 && currentState.playerCards.getOrNull(idx)?.currentHp?.let { it > 0 } == true) {
                                log.add(BattleLog("💀 ${card.name} yenildi!", false))
                            }
                        }
                    }
                }

                // Rakip bağlantısı kesildi mi?
                if (!onlineState.opponentConnected && onlineState.status == "active") {
                    Log.d("BattleVM", "🔌 Opponent disconnected!")
                    val myKey = if (onlineState.isPlayer1) "player1" else "player2"
                    val addLog = currentState.battleLog.toMutableList()
                    addLog.add(BattleLog("🔌 Rakip savaştan ayrıldı!", true))
                    _battleState.value = currentState.copy(battleLog = addLog)
                    OnlineBattleService.claimVictory(onlineState.battleId, myKey)
                    return@collect
                }

                // Savaş bitti mi?
                if (onlineState.status == "finished") {
                    val myKey = if (onlineState.isPlayer1) "player1" else "player2"
                    val won = onlineState.winner == myKey
                    turnTimerJob?.cancel()
                    finishBattle(won = won, enemyName = onlineState.opponentName, isOnline = true)
                    OnlineBattleService.leaveBattle()
                    return@collect
                }

                // Sıra değişti mi? Tur zamanlayıcıyı güncelle
                val turnChanged = currentState.isPlayerTurn != onlineState.isMyTurn
                
                _battleState.value = currentState.copy(
                    playerCards = myCards,
                    enemyCards = opCards,
                    playerActiveIndex = onlineState.myActiveIndex,
                    enemyActiveIndex = onlineState.opponentActiveIndex,
                    isPlayerTurn = onlineState.isMyTurn,
                    battleLog = log,
                    enemyName = onlineState.opponentName,
                    turnTimeRemaining = if (turnChanged) TURN_TIME_LIMIT else currentState.turnTimeRemaining
                )

                // Benim sıram başladıysa timer başlat
                if (turnChanged && onlineState.isMyTurn) {
                    startTurnTimer(isOnline = true)
                }
            }
        }
    }

    /**
     * Tur zamanlayıcı — 15 saniye içinde saldırmazsan otomatik saldırı.
     */
    private fun startTurnTimer(isOnline: Boolean) {
        turnTimerJob?.cancel()
        turnTimerJob = viewModelScope.launch {
            for (remaining in TURN_TIME_LIMIT downTo 1) {
                val state = _battleState.value
                if (state !is BattleState.InBattle || !state.isPlayerTurn) return@launch

                _battleState.value = state.copy(turnTimeRemaining = remaining)
                delay(1000)
            }

            // Süre doldu — otomatik saldırı!
            val state = _battleState.value
            if (state is BattleState.InBattle && state.isPlayerTurn) {
                Log.d("BattleVM", "⏰ Turn timer expired — auto-attacking!")
                if (isOnline) {
                    autoAttackOnline(state)
                } else {
                    autoAttackBot(state)
                }
            }
        }
    }

    /**
     * Savaş aramayı başlat.
     */
    fun startBattle() {
        viewModelScope.launch {
            val uid = authRepository.currentUser?.uid ?: return@launch
            val user = UserStateManager.currentUser() ?: return@launch

            // 1. Altın kontrolü
            if (user.stepGold < BATTLE_ENTRY_FEE) {
                _battleState.value = BattleState.InsufficientGold(
                    required = BATTLE_ENTRY_FEE,
                    current = user.stepGold
                )
                return@launch
            }

            // 2. Giriş ücretini düş
            UserStateManager.updateUser(mapOf(
                "stepGold" to (user.stepGold - BATTLE_ENTRY_FEE)
            ))

            _battleState.value = BattleState.Searching(60)

            // 3. Kartları yükle ve enerji kontrolü
            try {
                val snapshot = firestore.collection("users")
                    .document(uid).collection("cards").get().await()

                val allCardMap = Card.allCardsMap()

                // Manuel parse — energy alanlarını doğru okumak için
                data class DeckCardInfo(
                    val docId: String,
                    val cardId: String,
                    val level: Int,
                    val deckPosition: Int,
                    val currentEnergy: Int
                )

                val deckCards = mutableListOf<DeckCardInfo>()
                for (doc in snapshot.documents) {
                    val data = doc.data ?: continue
                    val inDeck = data["inDeck"] as? Boolean ?: false
                    if (!inDeck) continue

                    val cardId = data["cardId"] as? String ?: doc.id
                    val level = (data["level"] as? Number)?.toInt() ?: 1
                    val deckPos = (data["deckPosition"] as? Number)?.toInt() ?: 0
                    val energy = (data["energy"] as? Number)?.toInt() ?: 10
                    val maxEnergy = (data["maxEnergy"] as? Number)?.toInt() ?: 10
                    val lastRefill = (data["lastEnergyRefill"] as? Number)?.toLong() ?: System.currentTimeMillis()

                    val elapsed = System.currentTimeMillis() - lastRefill
                    val regenCount = (elapsed / (30 * 60 * 1000)).toInt()
                    val currentEnergy = (energy + regenCount).coerceAtMost(maxEnergy)

                    deckCards.add(DeckCardInfo(doc.id, cardId, level, deckPos, currentEnergy))
                }

                deckCards.sortBy { it.deckPosition }

                if (deckCards.isEmpty()) {
                    UserStateManager.updateUser(mapOf("stepGold" to user.stepGold))
                    _battleState.value = BattleState.Idle
                    return@launch
                }

                // Enerji kontrolü
                for (dc in deckCards) {
                    if (dc.currentEnergy <= 0) {
                        UserStateManager.updateUser(mapOf("stepGold" to user.stepGold))
                        val name = allCardMap[dc.cardId]?.name ?: "Kart"
                        _battleState.value = BattleState.NoEnergy(name)
                        return@launch
                    }
                }

                // BattleCard listesi oluştur
                val playerCards = deckCards.mapNotNull { dc ->
                    val card = allCardMap[dc.cardId] ?: return@mapNotNull null
                    BattleCard(
                        name = card.name,
                        emoji = card.emoji,
                        type = card.type,
                        maxHp = card.baseHp + (dc.level - 1) * 10,
                        currentHp = card.baseHp + (dc.level - 1) * 10,
                        attack = card.baseAttack + (dc.level - 1) * 5,
                        defense = card.baseDefense + (dc.level - 1) * 3,
                        speed = card.baseSpeed + (dc.level - 1),
                        level = dc.level
                    )
                }

                // 4. Savaşa başla
                val deckCardMaps = playerCards.map { card ->
                    mapOf(
                        "name" to card.name,
                        "emoji" to card.emoji,
                        "type" to card.type,
                        "maxHp" to card.maxHp,
                        "currentHp" to card.currentHp,
                        "attack" to card.attack,
                        "defense" to card.defense,
                        "speed" to card.speed,
                        "level" to card.level
                    )
                }

                MatchmakingService.startSearching(
                    uid = uid,
                    username = user.username,
                    trophies = user.trophies,
                    deckCards = deckCardMaps
                )

            } catch (e: Exception) {
                Log.e("BattleVM", "startBattle failed", e)
                UserStateManager.updateUser(mapOf("stepGold" to user.stepGold))
                _battleState.value = BattleState.Idle
            }
        }
    }

    fun cancelSearch() {
        // İptal edilince giriş ücretini iade et
        viewModelScope.launch {
            val user = UserStateManager.currentUser()
            if (user != null) {
                UserStateManager.updateUser(mapOf(
                    "stepGold" to (user.stepGold + BATTLE_ENTRY_FEE)
                ))
            }
        }
        MatchmakingService.stopSearching()
        _battleState.value = BattleState.Idle
    }

    private fun startOnlineBattle(battleId: String, isPlayer1: Boolean) {
        val uid = authRepository.currentUser?.uid ?: return
        Log.d("BattleVM", "Starting online battle: $battleId (isPlayer1=$isPlayer1)")

        OnlineBattleService.joinBattle(battleId, isPlayer1, uid)

        _battleState.value = BattleState.InBattle(
            playerCards = emptyList(),
            enemyCards = emptyList(),
            enemyName = "Bağlanıyor...",
            isOnline = true,
            battleId = battleId,
            isPlayer1 = isPlayer1,
            battleLog = listOf(BattleLog("🌐 Online savaş başlıyor!", false))
        )

        // player1 ilk sırayı alır
        if (isPlayer1) {
            startTurnTimer(isOnline = true)
        }
    }

    private fun startBotBattle() {
        viewModelScope.launch {
            val playerCards = loadPlayerCards()
            if (playerCards.isEmpty()) {
                _battleState.value = BattleState.Idle
                return@launch
            }

            val enemyName = "🤖 " + aiNames.random()
            val enemyCards = generateEnemyCards(playerCards)

            _battleState.value = BattleState.InBattle(
                playerCards = playerCards,
                enemyCards = enemyCards,
                enemyName = enemyName,
                isOnline = false,
                battleLog = listOf(
                    BattleLog("🤖 Rakip bulunamadı — $enemyName ile savaş!", false)
                )
            )

            // Bot savaşında da tur zamanlayıcı başlat
            startTurnTimer(isOnline = false)
        }
    }

    /**
     * Saldırı — bot veya online.
     */
    fun attack() {
        val state = _battleState.value
        if (state !is BattleState.InBattle || !state.isPlayerTurn) return

        turnTimerJob?.cancel()

        if (state.isOnline) {
            onlineAttack(state, isAutoAttack = false)
        } else {
            botAttack(state, isAutoAttack = false)
        }
    }

    /**
     * Online otomatik saldırı (süre doldu).
     */
    private fun autoAttackOnline(state: BattleState.InBattle) {
        val log = state.battleLog.toMutableList()
        log.add(BattleLog("⏰ Süre doldu — otomatik saldırı! (yarı hasar)", true))
        _battleState.value = state.copy(battleLog = log)
        onlineAttack(state, isAutoAttack = true)
    }

    /**
     * Bot otomatik saldırı (süre doldu).
     */
    private fun autoAttackBot(state: BattleState.InBattle) {
        val log = state.battleLog.toMutableList()
        log.add(BattleLog("⏰ Süre doldu — otomatik saldırı! (yarı hasar)", true))
        _battleState.value = state.copy(battleLog = log)
        botAttack(state, isAutoAttack = true)
    }

    /**
     * Online saldırı.
     */
    private fun onlineAttack(state: BattleState.InBattle, isAutoAttack: Boolean) {
        val myCard = state.playerCards.getOrNull(state.playerActiveIndex) ?: return
        val opCard = state.enemyCards.getOrNull(state.enemyActiveIndex) ?: return

        val myKey = if (state.isPlayer1) "player1" else "player2"
        val opKey = if (state.isPlayer1) "player2" else "player1"

        val opOnlineCards = OnlineBattleService.battleState.value?.opponentCards ?: return

        OnlineBattleService.submitAttack(
            battleId = state.battleId,
            attackerAttack = myCard.attack,
            defenderDefense = opCard.defense,
            defenderCurrentHp = opCard.currentHp,
            defenderCardIndex = state.enemyActiveIndex,
            defenderCards = opOnlineCards,
            myKey = myKey,
            opKey = opKey,
            isAutoAttack = isAutoAttack
        )
    }

    /**
     * Bot saldırı.
     */
    private fun botAttack(state: BattleState.InBattle, isAutoAttack: Boolean) {
        viewModelScope.launch {
            val playerCard = state.playerCards[state.playerActiveIndex]
            val enemyCard = state.enemyCards[state.enemyActiveIndex]

            // Oyuncu saldırır
            var damage = calculateDamage(playerCard.attack, enemyCard.defense)
            if (isAutoAttack) {
                damage = (damage * 0.5).toInt().coerceAtLeast(1)
            }
            val newEnemyHp = (enemyCard.currentHp - damage).coerceAtLeast(0)

            val updatedEnemyCards = state.enemyCards.toMutableList()
            updatedEnemyCards[state.enemyActiveIndex] = enemyCard.copy(currentHp = newEnemyHp)

            val log = state.battleLog.toMutableList()
            log.add(BattleLog(
                "${playerCard.emoji} ${playerCard.name} → $damage hasar!${if (isAutoAttack) " (oto)" else ""}",
                true
            ))

            var newEnemyActiveIndex = state.enemyActiveIndex
            if (newEnemyHp <= 0) {
                log.add(BattleLog("💀 ${enemyCard.name} yenildi!", true))

                val nextAlive = updatedEnemyCards.indexOfFirst { it.currentHp > 0 }
                if (nextAlive == -1) {
                    _battleState.value = state.copy(
                        enemyCards = updatedEnemyCards,
                        battleLog = log
                    )
                    delay(1000)
                    finishBattle(won = true, enemyName = state.enemyName, isOnline = false)
                    return@launch
                }
                newEnemyActiveIndex = nextAlive
                log.add(BattleLog("${updatedEnemyCards[nextAlive].emoji} ${updatedEnemyCards[nextAlive].name} sahaya çıktı!", false))
            }

            _battleState.value = state.copy(
                enemyCards = updatedEnemyCards,
                enemyActiveIndex = newEnemyActiveIndex,
                isPlayerTurn = false,
                battleLog = log
            )

            // Bot sırası
            delay(1200)
            enemyBotAttack()
        }
    }

    private fun enemyBotAttack() {
        val state = _battleState.value
        if (state !is BattleState.InBattle) return

        viewModelScope.launch {
            val enemyCard = state.enemyCards[state.enemyActiveIndex]
            val playerCard = state.playerCards[state.playerActiveIndex]

            val damage = calculateDamage(enemyCard.attack, playerCard.defense)
            val newPlayerHp = (playerCard.currentHp - damage).coerceAtLeast(0)

            val updatedPlayerCards = state.playerCards.toMutableList()
            updatedPlayerCards[state.playerActiveIndex] = playerCard.copy(currentHp = newPlayerHp)

            val log = state.battleLog.toMutableList()
            log.add(BattleLog(
                "${enemyCard.emoji} ${enemyCard.name} → $damage hasar!",
                false
            ))

            var newPlayerActiveIndex = state.playerActiveIndex
            if (newPlayerHp <= 0) {
                log.add(BattleLog("💀 ${playerCard.name} yenildi!", false))

                val nextAlive = updatedPlayerCards.indexOfFirst { it.currentHp > 0 }
                if (nextAlive == -1) {
                    _battleState.value = state.copy(
                        playerCards = updatedPlayerCards,
                        battleLog = log
                    )
                    delay(1000)
                    finishBattle(won = false, enemyName = state.enemyName, isOnline = false)
                    return@launch
                }
                newPlayerActiveIndex = nextAlive
                log.add(BattleLog("${updatedPlayerCards[nextAlive].emoji} ${updatedPlayerCards[nextAlive].name} sahaya çıktı!", true))
            }

            _battleState.value = state.copy(
                playerCards = updatedPlayerCards,
                playerActiveIndex = newPlayerActiveIndex,
                isPlayerTurn = true,
                battleLog = log,
                turnTimeRemaining = TURN_TIME_LIMIT
            )

            // Oyuncunun sırası başladı — timer
            startTurnTimer(isOnline = false)
        }
    }

    private fun finishBattle(won: Boolean, enemyName: String, isOnline: Boolean) {
        turnTimerJob?.cancel()
        val trophyChange = if (won) Random.nextInt(20, 35) else -Random.nextInt(10, 20)
        // Online: kazanan 35 altin alir (ikisi de 20 koydu, kazanan alir)
        // Bot: kazanirsan 35, kaybedersen 0 (bot parasi yok)
        val goldReward = if (won) BATTLE_WINNER_PRIZE else 0

        _battleState.value = BattleState.Finished(
            won = won,
            trophyChange = trophyChange,
            goldReward = goldReward,
            enemyName = enemyName,
            isOnline = isOnline
        )

        saveBattleResults(won, trophyChange, goldReward)
    }

    private fun saveBattleResults(won: Boolean, trophyChange: Int, goldReward: Int) {
        viewModelScope.launch {
            try {
                val user = UserStateManager.currentUser() ?: return@launch
                val newTrophies = (user.trophies + trophyChange).coerceAtLeast(0)
                val newGold = user.stepGold + goldReward
                val newXp = user.xp + if (won) 25 else 10

                UserStateManager.updateUser(mapOf(
                    "trophies" to newTrophies,
                    "stepGold" to newGold,
                    "xp" to newXp
                ))

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Maç sonunda destedeki kartların enerjisini düş
                try {
                    val snapshot = firestore.collection("users")
                        .document(uid).collection("cards").get().await()

                    for (doc in snapshot.documents) {
                        val data = doc.data ?: continue
                        val inDeck = data["inDeck"] as? Boolean ?: false
                        if (!inDeck) continue

                        val energy = (data["energy"] as? Number)?.toInt() ?: 10
                        val maxEnergy = (data["maxEnergy"] as? Number)?.toInt() ?: 10
                        val lastRefill = (data["lastEnergyRefill"] as? Number)?.toLong() ?: System.currentTimeMillis()

                        // Zamana göre gerçek enerjiyi hesapla
                        val elapsed = System.currentTimeMillis() - lastRefill
                        val regenCount = (elapsed / (30 * 60 * 1000)).toInt()
                        val currentEnergy = (energy + regenCount).coerceAtMost(maxEnergy)

                        if (currentEnergy > 0) {
                            val newEnergy = currentEnergy - 1
                            val newRefill = System.currentTimeMillis()

                            firestore.collection("users").document(uid)
                                .collection("cards").document(doc.id)
                                .update(
                                    "energy", newEnergy,
                                    "lastEnergyRefill", newRefill
                                ).await()

                            Log.d("BattleVM", "Card ${doc.id} energy: $currentEnergy -> $newEnergy")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BattleVM", "Energy deduction failed", e)
                }

                if (won) {
                    val chestType = if (Random.nextInt(100) < 70) "BRONZE" else "SILVER"
                    val chestId = firestore.collection("users")
                        .document(uid).collection("chests").document().id
                    val chest = UserChest(
                        id = chestId,
                        chestType = chestType,
                        earnedAt = System.currentTimeMillis(),
                        opened = false
                    )
                    firestore.collection("users")
                        .document(uid).collection("chests")
                        .document(chestId).set(chest.toMap()).await()
                }
            } catch (_: Exception) {}
        }
    }

    fun resetBattle() {
        turnTimerJob?.cancel()
        MatchmakingService.resetState()
        OnlineBattleService.leaveBattle()
        _battleState.value = BattleState.Idle
    }

    /**
     * Yeni hasar formülü — kart güçlerine daha yakın.
     * defense/4 (eskiden /2) ve minimum attack/3 garantisi.
     */
    private fun calculateDamage(attack: Int, defense: Int): Int {
        val baseDamage = (attack - defense / 4).coerceAtLeast(attack / 3)
        val variance = Random.nextInt(-2, 6)
        return (baseDamage + variance).coerceAtLeast(1)
    }

    private suspend fun loadPlayerCards(): List<BattleCard> {
        val uid = authRepository.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = firestore.collection("users")
                .document(uid).collection("cards").get().await()

            val userCards = snapshot.documents.mapNotNull { it.toObject<UserCard>() }
            val allCardMap = Card.allCardsMap()

            userCards.filter { it.inDeck }.sortedBy { it.deckPosition }.mapNotNull { uc ->
                val card = allCardMap[uc.cardId] ?: return@mapNotNull null
                BattleCard(
                    name = card.name,
                    emoji = card.emoji,
                    type = card.type,
                    maxHp = card.baseHp + (uc.level - 1) * 10,
                    currentHp = card.baseHp + (uc.level - 1) * 10,
                    attack = card.baseAttack + (uc.level - 1) * 5,
                    defense = card.baseDefense + (uc.level - 1) * 3,
                    speed = card.baseSpeed + (uc.level - 1),
                    level = uc.level
                )
            }.ifEmpty {
                Card.getStarterCards().map { card ->
                    BattleCard(card.name, card.emoji, card.type,
                        card.baseHp, card.baseHp, card.baseAttack,
                        card.baseDefense, card.baseSpeed, 1)
                }
            }
        } catch (_: Exception) {
            Card.getStarterCards().map { card ->
                BattleCard(card.name, card.emoji, card.type,
                    card.baseHp, card.baseHp, card.baseAttack,
                    card.baseDefense, card.baseSpeed, 1)
            }
        }
    }

    private fun generateEnemyCards(playerCards: List<BattleCard>): List<BattleCard> {
        val avgLevel = playerCards.map { it.level }.average().toInt().coerceAtLeast(1)
        val allCards = Card.getAllCards().shuffled()
        val enemyPool = allCards.take(4)

        return enemyPool.map { card ->
            val level = (avgLevel + Random.nextInt(-1, 2)).coerceAtLeast(1)
            BattleCard(
                name = card.name,
                emoji = card.emoji,
                type = card.type,
                maxHp = card.baseHp + (level - 1) * 10,
                currentHp = card.baseHp + (level - 1) * 10,
                attack = card.baseAttack + (level - 1) * 5,
                defense = card.baseDefense + (level - 1) * 3,
                speed = card.baseSpeed + (level - 1),
                level = level
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        turnTimerJob?.cancel()
        MatchmakingService.stopSearching()
        // removeListener kullan, leaveBattle DEĞİL!
        // Böylece onDisconnect handler'ları aktif kalır
        // ve Firebase oyuncunun çıktığını algılar.
        OnlineBattleService.removeListener()
    }
}
