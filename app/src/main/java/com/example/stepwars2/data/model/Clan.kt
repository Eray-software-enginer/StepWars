package com.example.stepwars2.data.model

data class Clan(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val badge: String = "⚔️",
    val leaderUid: String = "",
    val leaderName: String = "",
    val memberCount: Int = 0,
    val totalPower: Int = 0,
    val minTrophies: Int = 0,
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "badge" to badge,
        "leaderUid" to leaderUid,
        "leaderName" to leaderName,
        "memberCount" to memberCount,
        "totalPower" to totalPower,
        "minTrophies" to minTrophies,
        "isPublic" to isPublic,
        "createdAt" to createdAt
    )
}

data class ClanMember(
    val uid: String = "",
    val username: String = "",
    val level: Int = 1,
    val trophies: Int = 0,
    val role: String = "member", // leader, admin, member
    val joinedAt: Long = System.currentTimeMillis(),
    val power: Int = 0
) {
    constructor() : this(uid = "")

    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "username" to username,
        "level" to level,
        "trophies" to trophies,
        "role" to role,
        "joinedAt" to joinedAt,
        "power" to power
    )

    fun getRoleDisplayName(): String = when (role) {
        "leader" -> "Lider"
        "admin" -> "Yönetici"
        else -> "Üye"
    }

    fun canKick(): Boolean = role == "leader" || role == "admin"
}

data class ClanMessage(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "senderUid" to senderUid,
        "senderName" to senderName,
        "text" to text,
        "timestamp" to timestamp
    )
}
