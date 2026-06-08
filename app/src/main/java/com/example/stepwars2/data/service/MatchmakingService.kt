package com.example.stepwars2.data.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Gerçek zamanlı eşleşme servisi.
 * 
 * Akış:
 * 1. Oyuncu matchmaking/{uid} noduna yazar
 * 2. Tüm matchmaking/ nodunu dinler → rakip arar
 * 3. Küçük UID battle oluşturur → diğerine battleId yazar
 * 4. 60s sonra timeout → bot savaşı
 */
object MatchmakingService {

    private const val TAG = "Matchmaking"
    private const val DB_URL = "https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app"
    private const val TROPHY_RANGE = 9999 // Test için çok geniş

    private val database = FirebaseDatabase.getInstance(DB_URL)
    private val matchmakingRef = database.reference.child("matchmaking")
    private val battlesRef = database.reference.child("battles")

    sealed class MatchState {
        object Idle : MatchState()
        data class Searching(val secondsRemaining: Int = 60) : MatchState()
        data class Found(val battleId: String, val isPlayer1: Boolean) : MatchState()
        object Timeout : MatchState()
        data class Error(val message: String) : MatchState()
    }

    private val _state = MutableStateFlow<MatchState>(MatchState.Idle)
    val state: StateFlow<MatchState> = _state.asStateFlow()

    private var queueListener: ValueEventListener? = null
    private var myNodeListener: ValueEventListener? = null
    private var timeoutThread: Thread? = null
    private var isSearching = false
    private var myUid: String? = null
    private var battleCreated = false

    fun startSearching(
        uid: String,
        username: String,
        trophies: Int,
        deckCards: List<Map<String, Any>>
    ) {
        if (isSearching) return
        isSearching = true
        battleCreated = false
        myUid = uid
        _state.value = MatchState.Searching(60)

        Log.d(TAG, "=== MATCHMAKING START === uid=$uid, username=$username, trophies=$trophies")

        val playerData = mapOf(
            "uid" to uid,
            "username" to username,
            "trophies" to trophies,
            "cards" to deckCards,
            "timestamp" to System.currentTimeMillis()
        )

        // 1. Kuyruğa yaz
        matchmakingRef.child(uid).setValue(playerData)
            .addOnSuccessListener { 
                Log.d(TAG, "✅ Written to matchmaking queue")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ WRITE FAILED: ${e.message}")
                _state.value = MatchState.Error("Kuyruğa yazılamadı: ${e.message}")
                isSearching = false
            }

        // 2. Tüm kuyruğu dinle — başka oyuncu var mı?
        queueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isSearching || battleCreated) return
                
                val childCount = snapshot.childrenCount
                Log.d(TAG, "📡 Queue update: $childCount players in queue")
                
                for (child in snapshot.children) {
                    Log.d(TAG, "  → Player: ${child.key}")
                }

                for (child in snapshot.children) {
                    val otherUid = child.key ?: continue
                    if (otherUid == uid) continue

                    val otherTrophies = child.child("trophies").getValue(Int::class.java) ?: 0
                    val otherName = child.child("username").getValue(String::class.java) ?: "?"
                    
                    Log.d(TAG, "🎯 Potential match: $otherName ($otherUid), trophies=$otherTrophies")

                    if (kotlin.math.abs(otherTrophies - trophies) <= TROPHY_RANGE) {
                        if (uid < otherUid) {
                            Log.d(TAG, "🔨 I'm creator (my uid < other). Creating battle...")
                            battleCreated = true
                            createBattle(uid, otherUid, snapshot)
                        } else {
                            Log.d(TAG, "⏳ Waiting for $otherUid to create battle (their uid < mine)")
                        }
                        return
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ QUEUE LISTENER CANCELLED: ${error.message} (code=${error.code})")
                Log.e(TAG, "   matchmaking/ needs: .read: auth != null, .write: auth != null")
                // Error state yayınlama — timeout beklesin, bot o zaman gelsin
            }
        }
        matchmakingRef.addValueEventListener(queueListener!!)

        // 3. Kendi nodumda battleId dinle (diğer oyuncu tarafından yazılır)
        myNodeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isSearching) return
                val battleId = snapshot.child("battleId").getValue(String::class.java)
                val amPlayer1 = snapshot.child("isPlayer1").getValue(Boolean::class.java)
                if (battleId != null && amPlayer1 != null) {
                    Log.d(TAG, "🎉 Battle assigned to me: $battleId (isPlayer1=$amPlayer1)")
                    cleanup()
                    matchmakingRef.child(uid).removeValue()
                    _state.value = MatchState.Found(battleId, amPlayer1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "❌ MY NODE LISTENER CANCELLED: ${error.message}")
            }
        }
        matchmakingRef.child(uid).addValueEventListener(myNodeListener!!)

        // 4. 60s geri sayım
        startTimeoutCounter()
    }

    private fun startTimeoutCounter() {
        timeoutThread = Thread {
            try {
                var remaining = 60
                while (remaining > 0 && isSearching) {
                    _state.value = MatchState.Searching(remaining)
                    Thread.sleep(1000)
                    remaining--
                }
                if (isSearching) {
                    Log.d(TAG, "⏰ TIMEOUT — 60s passed, no match found")
                    val uid = myUid
                    cleanup()
                    if (uid != null) {
                        matchmakingRef.child(uid).removeValue()
                    }
                    _state.value = MatchState.Timeout
                }
            } catch (_: InterruptedException) { }
        }
        timeoutThread?.start()
    }

    private fun createBattle(myUid: String, otherUid: String, snapshot: DataSnapshot) {
        val player1Data = snapshot.child(myUid)
        val player2Data = snapshot.child(otherUid)

        val battleId = battlesRef.push().key ?: return

        Log.d(TAG, "Creating battle $battleId: $myUid vs $otherUid")

        val battleData = mapOf(
            "player1" to mapOf(
                "uid" to myUid,
                "username" to (player1Data.child("username").getValue(String::class.java) ?: "Oyuncu 1"),
                "trophies" to (player1Data.child("trophies").getValue(Int::class.java) ?: 0),
                "cards" to snapshotToList(player1Data.child("cards")),
                "activeCardIndex" to 0
            ),
            "player2" to mapOf(
                "uid" to otherUid,
                "username" to (player2Data.child("username").getValue(String::class.java) ?: "Oyuncu 2"),
                "trophies" to (player2Data.child("trophies").getValue(Int::class.java) ?: 0),
                "cards" to snapshotToList(player2Data.child("cards")),
                "activeCardIndex" to 0
            ),
            "currentTurn" to "player1",
            "status" to "active",
            "winner" to "",
            "createdAt" to System.currentTimeMillis()
        )

        battlesRef.child(battleId).setValue(battleData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Battle $battleId created!")

                // Diğer oyuncuya battleId bildir
                matchmakingRef.child(otherUid).child("battleId").setValue(battleId)
                matchmakingRef.child(otherUid).child("isPlayer1").setValue(false)

                Log.d(TAG, "✅ Notified $otherUid about battle")

                // Kendi kuyruğumu sil
                matchmakingRef.child(myUid).removeValue()

                cleanup()
                _state.value = MatchState.Found(battleId, true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Battle creation FAILED: ${e.message}")
                battleCreated = false
            }
    }

    private fun snapshotToList(snapshot: DataSnapshot): List<Any> {
        val list = mutableListOf<Any>()
        for (child in snapshot.children) {
            val map = mutableMapOf<String, Any>()
            for (field in child.children) {
                field.value?.let { map[field.key ?: ""] = it }
            }
            list.add(map)
        }
        return list
    }

    fun stopSearching() {
        Log.d(TAG, "=== MATCHMAKING STOP ===")
        val uid = myUid ?: FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            matchmakingRef.child(uid).removeValue()
        }
        cleanup()
        _state.value = MatchState.Idle
    }

    private fun cleanup() {
        isSearching = false
        queueListener?.let { matchmakingRef.removeEventListener(it) }
        queueListener = null
        myNodeListener?.let {
            myUid?.let { uid -> matchmakingRef.child(uid).removeEventListener(it) }
        }
        myNodeListener = null
        timeoutThread?.interrupt()
        timeoutThread = null
    }

    fun resetState() {
        cleanup()
        battleCreated = false
        _state.value = MatchState.Idle
    }
}
