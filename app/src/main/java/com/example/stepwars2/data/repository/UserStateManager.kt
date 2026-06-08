package com.example.stepwars2.data.repository

import android.util.Log
import com.example.stepwars2.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Singleton: Kullanıcı verisini bellekte tutar ve Firebase Realtime Database ile senkronize eder.
 * Tüm sayfalar aynı StateFlow'u okur — sayfa geçişlerinde yeniden veri çekmez.
 */
object UserStateManager {

    private const val TAG = "UserStateManager"
    private const val DB_URL = "https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app"

    private val database = FirebaseDatabase.getInstance(DB_URL)
    private val auth = FirebaseAuth.getInstance()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private var listener: ValueEventListener? = null
    private var currentUid: String? = null

    /**
     * Kullanıcı verisini dinlemeye başla.
     */
    fun startListening() {
        val uid = auth.currentUser?.uid ?: run {
            Log.e(TAG, "startListening: No authenticated user")
            return
        }
        if (uid == currentUid && listener != null) {
            Log.d(TAG, "Already listening for uid: $uid")
            return
        }

        stopListening()
        currentUid = uid
        Log.d(TAG, "Starting RTDB listener for uid: $uid")

        val userRef = database.reference.child("users").child(uid)

        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange: exists=${snapshot.exists()}, value=${snapshot.value}")
                if (snapshot.exists()) {
                    try {
                        val user = snapshotToUser(snapshot)
                        _user.value = user
                        Log.d(TAG, "User loaded: gold=${user.stepGold}, gems=${user.gems}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user", e)
                    }
                } else {
                    // Kullanıcı RTDB'de yok → yeni oluştur
                    Log.d(TAG, "User not found, creating new user in RTDB...")
                    val firebaseUser = auth.currentUser ?: return
                    val newUser = User(
                        uid = firebaseUser.uid,
                        username = firebaseUser.displayName ?: "Savaşçı",
                        email = firebaseUser.email ?: ""
                    )
                    userRef.setValue(newUser.toMap())
                        .addOnSuccessListener { Log.d(TAG, "New user created!") }
                        .addOnFailureListener { Log.e(TAG, "Failed to create user", it) }
                }
                _isLoaded.value = true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Listener cancelled: ${error.message} (code: ${error.code})")
                // Yine de bir kullanıcı göster
                val firebaseUser = auth.currentUser
                if (firebaseUser != null && _user.value == null) {
                    _user.value = User(
                        uid = firebaseUser.uid,
                        username = firebaseUser.displayName ?: "Savaşçı",
                        email = firebaseUser.email ?: ""
                    )
                }
                _isLoaded.value = true
            }
        }

        userRef.addValueEventListener(listener!!)
    }

    /**
     * DataSnapshot'tan User oluştur (getValue bazen sorun çıkarır, elle parse edelim).
     */
    private fun snapshotToUser(snapshot: DataSnapshot): User {
        return User(
            uid = snapshot.child("uid").getValue(String::class.java) ?: "",
            username = snapshot.child("username").getValue(String::class.java) ?: "",
            email = snapshot.child("email").getValue(String::class.java) ?: "",
            avatarUrl = snapshot.child("avatarUrl").getValue(String::class.java) ?: "",
            level = snapshot.child("level").getValue(Int::class.java) ?: 1,
            xp = snapshot.child("xp").getValue(Int::class.java) ?: 0,
            stepGold = snapshot.child("stepGold").getValue(Int::class.java) ?: 100,
            gems = snapshot.child("gems").getValue(Int::class.java) ?: 10,
            totalSteps = snapshot.child("totalSteps").getValue(Long::class.java) ?: 0L,
            todaySteps = snapshot.child("todaySteps").getValue(Int::class.java) ?: 0,
            unconvertedSteps = snapshot.child("unconvertedSteps").getValue(Int::class.java) ?: 0,
            trophies = snapshot.child("trophies").getValue(Int::class.java) ?: 0,
            league = snapshot.child("league").getValue(String::class.java) ?: "bronze",
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
            lastActive = snapshot.child("lastActive").getValue(Long::class.java) ?: System.currentTimeMillis(),
            clanId = snapshot.child("clanId").getValue(String::class.java) ?: ""
        )
    }

    /**
     * Dinlemeyi durdur.
     */
    fun stopListening() {
        val uid = currentUid ?: return
        listener?.let {
            database.reference.child("users").child(uid).removeEventListener(it)
        }
        listener = null
        currentUid = null
        _user.value = null
        _isLoaded.value = false
        Log.d(TAG, "Stopped listening")
    }

    /**
     * Kullanıcı verisini güncelle. XP güncellenirse otomatik seviye atlama kontrol eder.
     */
    suspend fun updateUser(updates: Map<String, Any>) {
        val uid = auth.currentUser?.uid ?: run {
            Log.e(TAG, "updateUser: No authenticated user")
            return
        }
        try {
            val finalUpdates = updates.toMutableMap()

            // XP güncellemesi varsa seviye atlama kontrol et
            val newXp = finalUpdates["xp"] as? Int
            if (newXp != null) {
                val currentUser = _user.value
                var level = (finalUpdates["level"] as? Int) ?: currentUser?.level ?: 1
                var xp = newXp

                // Seviye atlama döngüsü (birden fazla seviye atlanabilir)
                var xpForNextLevel = level * 100
                while (xp >= xpForNextLevel) {
                    xp -= xpForNextLevel
                    level++
                    Log.d(TAG, "🎉 LEVEL UP! → Seviye $level (kalan XP: $xp)")
                    xpForNextLevel = level * 100
                }

                finalUpdates["xp"] = xp
                finalUpdates["level"] = level
            }

            database.reference.child("users").child(uid).updateChildren(finalUpdates).await()
            Log.d(TAG, "Updated user: $finalUpdates")
        } catch (e: Exception) {
            Log.e(TAG, "Update failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Mevcut kullanıcıyı döndürür.
     */
    fun currentUser(): User? = _user.value
}
