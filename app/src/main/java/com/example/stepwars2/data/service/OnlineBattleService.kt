package com.example.stepwars2.data.service

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OnlineBattleCard(
    val name: String = "",
    val emoji: String = "",
    val type: String = "",
    val maxHp: Int = 100,
    val currentHp: Int = 100,
    val attack: Int = 20,
    val defense: Int = 10,
    val speed: Int = 5,
    val level: Int = 1
)

data class OnlineBattleState(
    val battleId: String = "",
    val isPlayer1: Boolean = true,
    val myUid: String = "",
    val opponentUid: String = "",
    val opponentName: String = "",
    val myCards: List<OnlineBattleCard> = emptyList(),
    val opponentCards: List<OnlineBattleCard> = emptyList(),
    val myActiveIndex: Int = 0,
    val opponentActiveIndex: Int = 0,
    val isMyTurn: Boolean = true,
    val status: String = "active",
    val winner: String = "",
    val lastActionBy: String = "",
    val lastDamage: Int = 0,
    val turnStartedAt: Long = 0,
    val opponentConnected: Boolean = true
)

object OnlineBattleService {

    private const val TAG = "OnlineBattle"
    private const val DB_URL = "https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app"

    private val database = FirebaseDatabase.getInstance(DB_URL)
    private val battlesRef = database.reference.child("battles")

    private val _battleState = MutableStateFlow<OnlineBattleState?>(null)
    val battleState: StateFlow<OnlineBattleState?> = _battleState.asStateFlow()

    private var battleListener: ValueEventListener? = null
    private var currentBattleId: String? = null
    private var amIPlayer1: Boolean = true

    /**
     * Savaş odasına katıl ve dinlemeye başla.
     * onDisconnect ile oyuncu çıkarsa diğeri otomatik kazanır.
     */
    fun joinBattle(battleId: String, isPlayer1: Boolean, myUid: String) {
        removeListener()

        currentBattleId = battleId
        amIPlayer1 = isPlayer1
        Log.d(TAG, "Joining battle: $battleId (isPlayer1=$isPlayer1)")

        val myKey = if (isPlayer1) "player1" else "player2"
        val opKey = if (isPlayer1) "player2" else "player1"
        val battleRef = battlesRef.child(battleId)

        // ✅ Kendi bağlantı durumumu yaz
        battleRef.child("$myKey/connected").setValue(true)

        // ✅ onDisconnect — ben çıkarsam:
        // 1. connected = false olsun
        // 2. status = finished, winner = rakip
        battleRef.child("$myKey/connected").onDisconnect().setValue(false)
        battleRef.child("status").onDisconnect().setValue("finished")
        battleRef.child("winner").onDisconnect().setValue(opKey)
        Log.d(TAG, "onDisconnect handlers set")

        // Tur zamanını yaz
        battleRef.child("turnStartedAt").setValue(System.currentTimeMillis())

        battleListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "Battle $battleId no longer exists")
                    return
                }

                val myData = snapshot.child(myKey)
                val opData = snapshot.child(opKey)

                val myCards = parseCards(myData.child("cards"))
                val opCards = parseCards(opData.child("cards"))

                val currentTurn = snapshot.child("currentTurn").getValue(String::class.java) ?: "player1"
                val status = snapshot.child("status").getValue(String::class.java) ?: "active"
                val winner = snapshot.child("winner").getValue(String::class.java) ?: ""
                val turnStartedAt = snapshot.child("turnStartedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                val myActiveIdx = myData.child("activeCardIndex").getValue(Int::class.java) ?: 0
                val opActiveIdx = opData.child("activeCardIndex").getValue(Int::class.java) ?: 0

                val lastActionBy = snapshot.child("lastAction").child("by").getValue(String::class.java) ?: ""
                val lastDamage = snapshot.child("lastAction").child("damage").getValue(Int::class.java) ?: 0

                // Rakibin bağlantı durumu
                val opConnected = opData.child("connected").getValue(Boolean::class.java) ?: true

                _battleState.value = OnlineBattleState(
                    battleId = battleId,
                    isPlayer1 = isPlayer1,
                    myUid = myUid,
                    opponentUid = opData.child("uid").getValue(String::class.java) ?: "",
                    opponentName = opData.child("username").getValue(String::class.java) ?: "Rakip",
                    myCards = myCards,
                    opponentCards = opCards,
                    myActiveIndex = myActiveIdx,
                    opponentActiveIndex = opActiveIdx,
                    isMyTurn = currentTurn == myKey,
                    status = status,
                    winner = winner,
                    lastActionBy = lastActionBy,
                    lastDamage = lastDamage,
                    turnStartedAt = turnStartedAt,
                    opponentConnected = opConnected
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Battle listener cancelled: ${error.message}")
            }
        }

        battlesRef.child(battleId).addValueEventListener(battleListener!!)
    }

    /**
     * Saldırı gönder.
     */
    fun submitAttack(
        battleId: String,
        attackerAttack: Int,
        defenderDefense: Int,
        defenderCurrentHp: Int,
        defenderCardIndex: Int,
        defenderCards: List<OnlineBattleCard>,
        myKey: String,
        opKey: String,
        isAutoAttack: Boolean = false
    ) {
        val baseDamage = (attackerAttack - defenderDefense / 4).coerceAtLeast(attackerAttack / 3)
        val variance = kotlin.random.Random.nextInt(-2, 6)
        var damage = (baseDamage + variance).coerceAtLeast(1)

        if (isAutoAttack) {
            damage = (damage * 0.5).toInt().coerceAtLeast(1)
        }

        val newHp = (defenderCurrentHp - damage).coerceAtLeast(0)

        val battleRef = battlesRef.child(battleId)
        val updates = mutableMapOf<String, Any>()

        updates["$opKey/cards/$defenderCardIndex/currentHp"] = newHp
        updates["lastAction/by"] = myKey
        updates["lastAction/type"] = if (isAutoAttack) "auto_attack" else "attack"
        updates["lastAction/damage"] = damage
        updates["lastAction/timestamp"] = System.currentTimeMillis()
        updates["turnStartedAt"] = System.currentTimeMillis()

        if (newHp <= 0) {
            val nextAlive = findNextAliveCard(defenderCards, defenderCardIndex, newHp)
            if (nextAlive == -1) {
                updates["status"] = "finished"
                updates["winner"] = myKey
            } else {
                updates["$opKey/activeCardIndex"] = nextAlive
                updates["currentTurn"] = opKey
            }
        } else {
            updates["currentTurn"] = opKey
        }

        battleRef.updateChildren(updates)
            .addOnSuccessListener {
                Log.d(TAG, "${if (isAutoAttack) "⏰ Auto" else "⚔️"}: $damage dmg")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Attack failed: ${e.message}")
            }
    }

    private fun findNextAliveCard(
        cards: List<OnlineBattleCard>,
        deadIndex: Int,
        deadNewHp: Int
    ): Int {
        for (i in cards.indices) {
            if (i == deadIndex) continue
            if (cards[i].currentHp > 0) return i
        }
        return -1
    }

    /**
     * Rakip çıktı — ben kazandım.
     */
    fun claimVictory(battleId: String, myKey: String) {
        val updates = mapOf(
            "status" to "finished",
            "winner" to myKey
        )
        battlesRef.child(battleId).updateChildren(updates)
        Log.d(TAG, "Victory claimed: opponent disconnected")
    }

    fun surrenderBattle(battleId: String, myKey: String, opKey: String) {
        val updates = mapOf(
            "status" to "finished",
            "winner" to opKey
        )
        battlesRef.child(battleId).updateChildren(updates)
        leaveBattle()
    }

    /**
     * Savaş normal bitti — onDisconnect iptal et + listener temizle.
     */
    fun leaveBattle() {
        currentBattleId?.let { id ->
            // Normal çıkış → onDisconnect iptal et (çünkü savaş zaten bitti)
            val battleRef = battlesRef.child(id)
            val myKey = if (amIPlayer1) "player1" else "player2"
            battleRef.child("$myKey/connected").onDisconnect().cancel()
            battleRef.child("status").onDisconnect().cancel()
            battleRef.child("winner").onDisconnect().cancel()
        }
        removeListener()
    }

    /**
     * Sadece listener'ı kaldır — onDisconnect handler'larına DOKUNMA.
     * ViewModel onCleared olduğunda çağrılır, böylece onDisconnect aktif kalır.
     */
    fun removeListener() {
        currentBattleId?.let { id ->
            battleListener?.let { listener ->
                battlesRef.child(id).removeEventListener(listener)
            }
        }
        battleListener = null
        currentBattleId = null
        _battleState.value = null
    }

    private fun parseCards(cardsSnapshot: DataSnapshot): List<OnlineBattleCard> {
        val cards = mutableListOf<OnlineBattleCard>()
        for (child in cardsSnapshot.children) {
            val card = OnlineBattleCard(
                name = child.child("name").getValue(String::class.java) ?: "",
                emoji = child.child("emoji").getValue(String::class.java) ?: "⚔️",
                type = child.child("type").getValue(String::class.java) ?: "attacker",
                maxHp = child.child("maxHp").getValue(Int::class.java) ?: 100,
                currentHp = child.child("currentHp").getValue(Int::class.java) ?: 100,
                attack = child.child("attack").getValue(Int::class.java) ?: 20,
                defense = child.child("defense").getValue(Int::class.java) ?: 10,
                speed = child.child("speed").getValue(Int::class.java) ?: 5,
                level = child.child("level").getValue(Int::class.java) ?: 1
            )
            cards.add(card)
        }
        return cards
    }
}
