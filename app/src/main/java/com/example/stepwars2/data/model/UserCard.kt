package com.example.stepwars2.data.model

data class UserCard(
    val id: String = "",
    val cardId: String = "",
    val userId: String = "",
    val level: Int = 1,
    val count: Int = 1,
    val inDeck: Boolean = true,
    val deckPosition: Int = -1,
    val energy: Int = 10,
    val maxEnergy: Int = 10,
    val lastEnergyRefill: Long = System.currentTimeMillis()
) {
    constructor() : this(id = "")

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "cardId" to cardId,
        "userId" to userId,
        "level" to level,
        "count" to count,
        "inDeck" to inDeck,
        "deckPosition" to deckPosition,
        "energy" to energy,
        "maxEnergy" to maxEnergy,
        "lastEnergyRefill" to lastEnergyRefill
    )

    /**
     * Zamana göre yenilenen enerjiyi hesapla (her 30 dakikada 1 enerji).
     */
    fun currentEnergy(): Int {
        val elapsed = System.currentTimeMillis() - lastEnergyRefill
        val regenCount = (elapsed / (30 * 60 * 1000)).toInt() // 30 dakikada 1
        return (energy + regenCount).coerceAtMost(maxEnergy)
    }

    /**
     * Enerji harcandığında kaydedilecek yeni değerleri döndürür (energy, lastEnergyRefill)
     */
    fun useEnergy(amount: Int = 1): Pair<Int, Long> {
        val current = currentEnergy()
        val newEnergy = (current - amount).coerceAtLeast(0)
        
        val newRefill = if (current == maxEnergy) {
            System.currentTimeMillis()
        } else {
            val elapsed = System.currentTimeMillis() - lastEnergyRefill
            val regenCount = (elapsed / (30 * 60 * 1000)).toLong()
            lastEnergyRefill + (regenCount * 30 * 60 * 1000)
        }
        return Pair(newEnergy, newRefill)
    }

    /**
     * Enerji eklendiğinde kaydedilecek yeni değerleri döndürür
     */
    fun addEnergy(amount: Int): Pair<Int, Long> {
        val current = currentEnergy()
        val newEnergy = (current + amount).coerceAtMost(maxEnergy)
        val newRefill = if (newEnergy == maxEnergy) {
            System.currentTimeMillis()
        } else {
            val elapsed = System.currentTimeMillis() - lastEnergyRefill
            val regenCount = (elapsed / (30 * 60 * 1000)).toLong()
            lastEnergyRefill + (regenCount * 30 * 60 * 1000)
        }
        return Pair(newEnergy, newRefill)
    }
}
