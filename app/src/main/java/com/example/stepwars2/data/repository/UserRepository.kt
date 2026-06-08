package com.example.stepwars2.data.repository

import android.util.Log
import com.example.stepwars2.data.model.Card
import com.example.stepwars2.data.model.User
import com.example.stepwars2.data.model.UserCard
import com.example.stepwars2.data.model.UserChest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Kullanıcıyı RTDB'de oluştur + Firestore'a starter kartları ver.
     * RTDB kaydı UserStateManager tarafından otomatik yapılıyor,
     * bu fonksiyon sadece starter kartları + hoşgeldin sandığı verir.
     */
    suspend fun createUser(user: User): Result<Unit> {
        return try {
            giveStarterCards(user.uid)
            giveWelcomeChest(user.uid)
            Log.d(TAG, "User initialized with starter cards and welcome chest: ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "createUser failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Her girişte kartları kontrol et — yoksa ver.
     */
    suspend fun ensureUserHasCards(uid: String): Boolean {
        return try {
            val cardsSnapshot = usersCollection.document(uid)
                .collection("cards").limit(1).get().await()
            if (cardsSnapshot.isEmpty) {
                Log.d(TAG, "User has no cards, giving starters: $uid")
                giveStarterCards(uid)
                giveWelcomeChest(uid)
                true
            } else {
                Log.d(TAG, "User already has ${cardsSnapshot.size()} cards")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "ensureUserHasCards failed: ${e.message}", e)
            false
        }
    }

    suspend fun getUser(uid: String): Result<User> {
        return try {
            // Kullanıcı artık RTDB'de, UserStateManager'dan al
            val user = UserStateManager.currentUser()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            UserStateManager.updateUser(updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun userExists(uid: String): Result<Boolean> {
        return try {
            Result.success(UserStateManager.currentUser() != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 4 başlangıç kartı ver — hepsi desteye eklenir.
     */
    private suspend fun giveStarterCards(userId: String) {
        val starterCards = Card.getStarterCards()
        val cardsCollection = usersCollection.document(userId).collection("cards")

        starterCards.forEachIndexed { index, card ->
            try {
                val userCard = UserCard(
                    id = card.id,
                    cardId = card.id,
                    userId = userId,
                    level = 1,
                    count = 1,
                    inDeck = true,
                    deckPosition = index
                )
                cardsCollection.document(card.id).set(userCard.toMap()).await()
                Log.d(TAG, "Gave starter card: ${card.name} to $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to give card ${card.name}: ${e.message}")
            }
        }
    }

    /**
     * Hoşgeldin sandığı ver — 1 Gümüş sandık.
     */
    private suspend fun giveWelcomeChest(userId: String) {
        try {
            val chestId = usersCollection.document(userId)
                .collection("chests").document().id
            val chest = UserChest(
                id = chestId,
                chestType = "SILVER",
                earnedAt = System.currentTimeMillis(),
                opened = false
            )
            usersCollection.document(userId)
                .collection("chests").document(chestId)
                .set(chest.toMap()).await()
            Log.d(TAG, "Gave welcome chest to $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to give welcome chest: ${e.message}")
        }
    }
}
