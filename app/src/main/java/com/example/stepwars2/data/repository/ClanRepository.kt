package com.example.stepwars2.data.repository

import android.util.Log
import com.example.stepwars2.data.model.Clan
import com.example.stepwars2.data.model.ClanMember
import com.example.stepwars2.data.model.ClanMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ClanRepository {
    private val TAG = "ClanRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance("https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app")
    private val auth = FirebaseAuth.getInstance()
    private val clansRef = firestore.collection("clans")

    companion object {
        const val MAX_MEMBERS = 30
        const val CREATE_COST = 100
    }

    /**
     * Tüm klanları toplam güce göre sıralı getir
     */
    suspend fun getClans(): List<Clan> {
        return try {
            val snapshot = clansRef
                .orderBy("totalPower", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Clan(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    badge = data["badge"] as? String ?: "⚔️",
                    leaderUid = data["leaderUid"] as? String ?: "",
                    leaderName = data["leaderName"] as? String ?: "",
                    memberCount = (data["memberCount"] as? Number)?.toInt() ?: 0,
                    totalPower = (data["totalPower"] as? Number)?.toInt() ?: 0,
                    minTrophies = (data["minTrophies"] as? Number)?.toInt() ?: 0,
                    isPublic = data["isPublic"] as? Boolean ?: true,
                    createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getClans failed", e)
            emptyList()
        }
    }

    /**
     * Klan oluştur
     */
    suspend fun createClan(
        name: String,
        description: String,
        badge: String,
        minTrophies: Int
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val user = UserStateManager.currentUser() ?: return Result.failure(Exception("No user"))

        if (user.stepGold < CREATE_COST) {
            return Result.failure(Exception("Yeterli altın yok! (${CREATE_COST} altın gerekli)"))
        }
        if (user.clanId.isNotEmpty()) {
            return Result.failure(Exception("Zaten bir klana üyesin!"))
        }

        return try {
            val docRef = clansRef.document()
            val clanId = docRef.id

            val clan = Clan(
                id = clanId,
                name = name,
                description = description,
                badge = badge,
                leaderUid = uid,
                leaderName = user.username,
                memberCount = 1,
                totalPower = user.level * 10 + user.trophies,
                minTrophies = minTrophies,
                isPublic = true
            )

            val member = ClanMember(
                uid = uid,
                username = user.username,
                level = user.level,
                trophies = user.trophies,
                role = "leader",
                power = user.level * 10 + user.trophies
            )

            // Batch write
            firestore.runBatch { batch ->
                batch.set(docRef, clan.toMap())
                batch.set(docRef.collection("members").document(uid), member.toMap())
            }.await()

            // Update user clanId in RTDB + deduct gold
            UserStateManager.updateUser(mapOf(
                "clanId" to clanId,
                "stepGold" to (user.stepGold - CREATE_COST)
            ))

            Log.d(TAG, "Clan created: $clanId")
            Result.success(clanId)
        } catch (e: Exception) {
            Log.e(TAG, "createClan failed", e)
            Result.failure(e)
        }
    }

    /**
     * Klana katıl
     */
    suspend fun joinClan(clanId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val user = UserStateManager.currentUser() ?: return Result.failure(Exception("No user"))

        if (user.clanId.isNotEmpty()) {
            return Result.failure(Exception("Zaten bir klana üyesin!"))
        }

        return try {
            val clanDoc = clansRef.document(clanId).get().await()
            val data = clanDoc.data ?: return Result.failure(Exception("Klan bulunamadı"))
            val memberCount = (data["memberCount"] as? Number)?.toInt() ?: 0
            val minTrophies = (data["minTrophies"] as? Number)?.toInt() ?: 0

            if (memberCount >= MAX_MEMBERS) {
                return Result.failure(Exception("Klan dolu! (Maks $MAX_MEMBERS üye)"))
            }
            if (user.trophies < minTrophies) {
                return Result.failure(Exception("Minimum $minTrophies kupa gerekli!"))
            }

            val member = ClanMember(
                uid = uid,
                username = user.username,
                level = user.level,
                trophies = user.trophies,
                role = "member",
                power = user.level * 10 + user.trophies
            )

            firestore.runBatch { batch ->
                batch.set(clansRef.document(clanId).collection("members").document(uid), member.toMap())
                batch.update(clansRef.document(clanId),
                    "memberCount", memberCount + 1,
                    "totalPower", (data["totalPower"] as? Number)?.toInt()?.plus(member.power) ?: member.power
                )
            }.await()

            UserStateManager.updateUser(mapOf("clanId" to clanId))
            Log.d(TAG, "Joined clan: $clanId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "joinClan failed", e)
            Result.failure(e)
        }
    }

    /**
     * Klandan ayrıl
     */
    suspend fun leaveClan(clanId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val memberDoc = clansRef.document(clanId).collection("members").document(uid).get().await()
            val memberData = memberDoc.data ?: return Result.failure(Exception("Üyelik bulunamadı"))
            val role = memberData["role"] as? String ?: "member"

            if (role == "leader") {
                return Result.failure(Exception("Lider klandan ayrılamaz! Önce liderliği devret."))
            }

            val clanDoc = clansRef.document(clanId).get().await()
            val clanData = clanDoc.data ?: return Result.failure(Exception("Klan bulunamadı"))
            val memberCount = (clanData["memberCount"] as? Number)?.toInt() ?: 1
            val totalPower = (clanData["totalPower"] as? Number)?.toInt() ?: 0
            val memberPower = (memberData["power"] as? Number)?.toInt() ?: 0

            firestore.runBatch { batch ->
                batch.delete(clansRef.document(clanId).collection("members").document(uid))
                batch.update(clansRef.document(clanId),
                    "memberCount", (memberCount - 1).coerceAtLeast(0),
                    "totalPower", (totalPower - memberPower).coerceAtLeast(0)
                )
            }.await()

            UserStateManager.updateUser(mapOf("clanId" to ""))
            Log.d(TAG, "Left clan: $clanId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "leaveClan failed", e)
            Result.failure(e)
        }
    }

    /**
     * Klan bilgisini getir
     */
    suspend fun getClan(clanId: String): Clan? {
        return try {
            val doc = clansRef.document(clanId).get().await()
            val data = doc.data ?: return null
            Clan(
                id = doc.id,
                name = data["name"] as? String ?: "",
                description = data["description"] as? String ?: "",
                badge = data["badge"] as? String ?: "⚔️",
                leaderUid = data["leaderUid"] as? String ?: "",
                leaderName = data["leaderName"] as? String ?: "",
                memberCount = (data["memberCount"] as? Number)?.toInt() ?: 0,
                totalPower = (data["totalPower"] as? Number)?.toInt() ?: 0,
                minTrophies = (data["minTrophies"] as? Number)?.toInt() ?: 0,
                isPublic = data["isPublic"] as? Boolean ?: true,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "getClan failed", e)
            null
        }
    }

    /**
     * Üyeleri güce göre sıralı getir
     */
    suspend fun getMembers(clanId: String): List<ClanMember> {
        return try {
            val snapshot = clansRef.document(clanId)
                .collection("members")
                .orderBy("power", Query.Direction.DESCENDING)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                ClanMember(
                    uid = data["uid"] as? String ?: "",
                    username = data["username"] as? String ?: "",
                    level = (data["level"] as? Number)?.toInt() ?: 1,
                    trophies = (data["trophies"] as? Number)?.toInt() ?: 0,
                    role = data["role"] as? String ?: "member",
                    joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: 0L,
                    power = (data["power"] as? Number)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMembers failed", e)
            emptyList()
        }
    }

    /**
     * Üyeyi at (leader ve admin yapabilir)
     */
    suspend fun kickMember(clanId: String, targetUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

        return try {
            // Yetkiyi kontrol et
            val myMemberDoc = clansRef.document(clanId).collection("members").document(uid).get().await()
            val myRole = myMemberDoc.data?.get("role") as? String ?: "member"
            if (myRole != "leader" && myRole != "admin") {
                return Result.failure(Exception("Bu işlem için yetkiniz yok!"))
            }

            val targetDoc = clansRef.document(clanId).collection("members").document(targetUid).get().await()
            val targetData = targetDoc.data ?: return Result.failure(Exception("Üye bulunamadı"))
            val targetRole = targetData["role"] as? String ?: "member"
            if (targetRole == "leader") {
                return Result.failure(Exception("Lideri atamazsınız!"))
            }
            if (targetRole == "admin" && myRole != "leader") {
                return Result.failure(Exception("Yöneticiyi sadece lider atabilir!"))
            }

            val clanDoc = clansRef.document(clanId).get().await()
            val clanData = clanDoc.data ?: return Result.failure(Exception("Klan bulunamadı"))
            val memberCount = (clanData["memberCount"] as? Number)?.toInt() ?: 1
            val totalPower = (clanData["totalPower"] as? Number)?.toInt() ?: 0
            val targetPower = (targetData["power"] as? Number)?.toInt() ?: 0

            firestore.runBatch { batch ->
                batch.delete(clansRef.document(clanId).collection("members").document(targetUid))
                batch.update(clansRef.document(clanId),
                    "memberCount", (memberCount - 1).coerceAtLeast(0),
                    "totalPower", (totalPower - targetPower).coerceAtLeast(0)
                )
            }.await()

            // Kicked user's clanId in RTDB
            rtdb.reference.child("users").child(targetUid).child("clanId").setValue("").await()

            Log.d(TAG, "Kicked $targetUid from $clanId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "kickMember failed", e)
            Result.failure(e)
        }
    }

    /**
     * Üyeyi admin yap veya adminliğini al
     */
    suspend fun setMemberRole(clanId: String, targetUid: String, newRole: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val myMemberDoc = clansRef.document(clanId).collection("members").document(uid).get().await()
            val myRole = myMemberDoc.data?.get("role") as? String ?: "member"
            if (myRole != "leader") {
                return Result.failure(Exception("Sadece lider rol atayabilir!"))
            }

            clansRef.document(clanId).collection("members").document(targetUid)
                .update("role", newRole).await()

            Log.d(TAG, "Set $targetUid role to $newRole")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "setMemberRole failed", e)
            Result.failure(e)
        }
    }

    /**
     * Mesaj gönder
     */
    suspend fun sendMessage(clanId: String, text: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val user = UserStateManager.currentUser() ?: return Result.failure(Exception("No user"))

        return try {
            val msgRef = clansRef.document(clanId).collection("messages").document()
            val message = ClanMessage(
                id = msgRef.id,
                senderUid = uid,
                senderName = user.username,
                text = text
            )
            msgRef.set(message.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage failed", e)
            Result.failure(e)
        }
    }

    /**
     * Mesajları gerçek zamanlı dinle (son 50)
     */
    fun listenMessages(clanId: String): Flow<List<ClanMessage>> = callbackFlow {
        val registration: ListenerRegistration = clansRef.document(clanId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Message listen failed", error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ClanMessage(
                        id = doc.id,
                        senderUid = data["senderUid"] as? String ?: "",
                        senderName = data["senderName"] as? String ?: "",
                        text = data["text"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                    )
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Üyeleri gerçek zamanlı dinle
     */
    fun listenMembers(clanId: String): Flow<List<ClanMember>> = callbackFlow {
        val registration: ListenerRegistration = clansRef.document(clanId)
            .collection("members")
            .orderBy("power", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Members listen failed", error)
                    return@addSnapshotListener
                }
                val members = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    ClanMember(
                        uid = data["uid"] as? String ?: "",
                        username = data["username"] as? String ?: "",
                        level = (data["level"] as? Number)?.toInt() ?: 1,
                        trophies = (data["trophies"] as? Number)?.toInt() ?: 0,
                        role = data["role"] as? String ?: "member",
                        joinedAt = (data["joinedAt"] as? Number)?.toLong() ?: 0L,
                        power = (data["power"] as? Number)?.toInt() ?: 0
                    )
                } ?: emptyList()
                trySend(members)
            }
        awaitClose { registration.remove() }
    }
}
