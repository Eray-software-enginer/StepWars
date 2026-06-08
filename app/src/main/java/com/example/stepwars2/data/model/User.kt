package com.example.stepwars2.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val level: Int = 1,
    val xp: Int = 0,
    val stepGold: Int = 100,
    val gems: Int = 10,
    val totalSteps: Long = 0,
    val todaySteps: Int = 0,
    val unconvertedSteps: Int = 0,
    val trophies: Int = 0,
    val league: String = "bronze",
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis(),
    val clanId: String = ""
) {
    // No-arg constructor needed for Firestore
    constructor() : this(uid = "")
    
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "username" to username,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "level" to level,
        "xp" to xp,
        "stepGold" to stepGold,
        "gems" to gems,
        "totalSteps" to totalSteps,
        "todaySteps" to todaySteps,
        "unconvertedSteps" to unconvertedSteps,
        "trophies" to trophies,
        "league" to league,
        "createdAt" to createdAt,
        "lastActive" to lastActive,
        "clanId" to clanId
    )
}
