package com.example.stepwars2.data.model

enum class ChestType(
    val displayName: String,
    val emoji: String,
    val cardCount: Int,
    val guaranteedRarity: String?, // minimum guaranteed rarity
    val goldBonus: Int
) {
    BRONZE("Bronz Sandık", "📦", 2, null, 10),
    SILVER("Gümüş Sandık", "🗃️", 3, "rare", 25),
    GOLD("Altın Sandık", "✨", 4, "epic", 50),
    EPIC("Epik Sandık", "💎", 5, "epic", 100),
    LEGENDARY("Efsanevi Sandık", "🌟", 6, "legendary", 200)
}

data class UserChest(
    val id: String = "",
    val chestType: String = "BRONZE", // ChestType name
    val earnedAt: Long = System.currentTimeMillis(),
    val opened: Boolean = false
) {
    constructor() : this(id = "")

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "chestType" to chestType,
        "earnedAt" to earnedAt,
        "opened" to opened
    )

    fun getType(): ChestType = try {
        ChestType.valueOf(chestType)
    } catch (_: Exception) {
        ChestType.BRONZE
    }
}
