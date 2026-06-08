package com.example.stepwars2.domain.usecase

import com.example.stepwars2.data.model.Card
import com.example.stepwars2.data.model.ChestType
import kotlin.random.Random

data class ChestReward(
    val cards: List<Card>,
    val goldBonus: Int,
    val gemsBonus: Int = 0
)

object ChestRewardEngine {

    // All available cards in the game (expanded pool)
    private val allCards = listOf(
        // Common cards
        Card(id = "warrior_01", name = "Savaşçı", rarity = "common", type = "attacker", baseHp = 120, baseAttack = 25, baseDefense = 15, baseSpeed = 5, emoji = "⚔️", description = "Güçlü yakın dövüş savaşçısı"),
        Card(id = "archer_01", name = "Okçu", rarity = "common", type = "attacker", baseHp = 80, baseAttack = 35, baseDefense = 8, baseSpeed = 7, emoji = "🏹", description = "Uzak mesafeden saldırır"),
        Card(id = "shield_01", name = "Kalkan Muhafızı", rarity = "common", type = "tank", baseHp = 200, baseAttack = 10, baseDefense = 30, baseSpeed = 3, emoji = "🛡️", description = "Yüksek savunma"),
        Card(id = "healer_01", name = "Şifacı", rarity = "common", type = "healer", baseHp = 90, baseAttack = 12, baseDefense = 12, baseSpeed = 6, emoji = "💚", description = "Can yeniler"),
        Card(id = "knight_01", name = "Şövalye", rarity = "common", type = "tank", baseHp = 150, baseAttack = 20, baseDefense = 25, baseSpeed = 4, emoji = "🗡️", description = "Dengeli tank savaşçı"),
        Card(id = "scout_01", name = "Keşifçi", rarity = "common", type = "attacker", baseHp = 70, baseAttack = 28, baseDefense = 10, baseSpeed = 9, emoji = "🏃", description = "Hızlı ve çevik"),
        
        // Rare cards
        Card(id = "mage_01", name = "Büyücü", rarity = "rare", type = "attacker", baseHp = 100, baseAttack = 45, baseDefense = 10, baseSpeed = 6, emoji = "🧙", description = "Güçlü büyü saldırıları"),
        Card(id = "paladin_01", name = "Paladin", rarity = "rare", type = "tank", baseHp = 180, baseAttack = 22, baseDefense = 28, baseSpeed = 4, emoji = "⚜️", description = "Kutsal zırh taşır"),
        Card(id = "ninja_01", name = "Ninja", rarity = "rare", type = "attacker", baseHp = 85, baseAttack = 40, baseDefense = 12, baseSpeed = 10, emoji = "🥷", description = "Gölgelerden saldırır"),
        Card(id = "cleric_01", name = "Rahip", rarity = "rare", type = "healer", baseHp = 110, baseAttack = 15, baseDefense = 20, baseSpeed = 5, emoji = "✝️", description = "Güçlü şifa büyüleri"),
        Card(id = "berserker_01", name = "Berserker", rarity = "rare", type = "attacker", baseHp = 130, baseAttack = 38, baseDefense = 8, baseSpeed = 7, emoji = "🪓", description = "Öfkeyle güçlenir"),

        // Epic cards
        Card(id = "dragon_01", name = "Ejderha", rarity = "epic", type = "attacker", baseHp = 200, baseAttack = 55, baseDefense = 20, baseSpeed = 6, emoji = "🐉", description = "Ateş nefesi ile yakar"),
        Card(id = "golem_01", name = "Golem", rarity = "epic", type = "tank", baseHp = 350, baseAttack = 15, baseDefense = 40, baseSpeed = 2, emoji = "🗿", description = "Yıkılmaz kaya devri"),
        Card(id = "phoenix_01", name = "Anka Kuşu", rarity = "epic", type = "healer", baseHp = 150, baseAttack = 35, baseDefense = 15, baseSpeed = 8, emoji = "🔥", description = "Külllerinden yeniden doğar"),
        Card(id = "assassin_01", name = "Suikastçı", rarity = "epic", type = "attacker", baseHp = 100, baseAttack = 65, baseDefense = 5, baseSpeed = 12, emoji = "🗡️", description = "Kritik vuruş ustası"),

        // Legendary cards  
        Card(id = "zeus_01", name = "Zeus", rarity = "legendary", type = "attacker", baseHp = 250, baseAttack = 70, baseDefense = 25, baseSpeed = 8, emoji = "⚡", description = "Yıldırımlarla yok eder"),
        Card(id = "titan_01", name = "Titan", rarity = "legendary", type = "tank", baseHp = 500, baseAttack = 30, baseDefense = 50, baseSpeed = 3, emoji = "👹", description = "Devasa güç ve dayanıklılık"),
        Card(id = "angel_01", name = "Melek", rarity = "legendary", type = "healer", baseHp = 200, baseAttack = 40, baseDefense = 30, baseSpeed = 10, emoji = "👼", description = "İlahi güçle iyileştirir")
    )

    // Rarity weights for different chest types
    private val rarityWeights = mapOf(
        ChestType.BRONZE to mapOf("common" to 75, "rare" to 20, "epic" to 4, "legendary" to 1),
        ChestType.SILVER to mapOf("common" to 55, "rare" to 30, "epic" to 12, "legendary" to 3),
        ChestType.GOLD to mapOf("common" to 35, "rare" to 35, "epic" to 22, "legendary" to 8),
        ChestType.EPIC to mapOf("common" to 20, "rare" to 30, "epic" to 35, "legendary" to 15),
        ChestType.LEGENDARY to mapOf("common" to 10, "rare" to 20, "epic" to 35, "legendary" to 35)
    )

    fun openChest(chestType: ChestType): ChestReward {
        val cards = mutableListOf<Card>()
        val weights = rarityWeights[chestType] ?: rarityWeights[ChestType.BRONZE]!!

        // Generate random cards
        repeat(chestType.cardCount) { index ->
            val rarity = if (index == 0 && chestType.guaranteedRarity != null) {
                // First card is guaranteed rarity
                chestType.guaranteedRarity
            } else {
                rollRarity(weights)
            }
            val pool = allCards.filter { it.rarity == rarity }
            if (pool.isNotEmpty()) {
                cards.add(pool.random())
            } else {
                cards.add(allCards.filter { it.rarity == "common" }.random())
            }
        }

        // Sort by rarity (legendary last for dramatic reveal)
        val rarityOrder = mapOf("common" to 0, "rare" to 1, "epic" to 2, "legendary" to 3)
        cards.sortBy { rarityOrder[it.rarity] ?: 0 }

        val gemsBonus = when(chestType) {
            ChestType.GOLD -> 2
            ChestType.EPIC -> 5
            ChestType.LEGENDARY -> 15
            else -> 0
        }

        return ChestReward(
            cards = cards,
            goldBonus = chestType.goldBonus,
            gemsBonus = gemsBonus
        )
    }

    private fun rollRarity(weights: Map<String, Int>): String {
        val total = weights.values.sum()
        var roll = Random.nextInt(total)
        for ((rarity, weight) in weights) {
            roll -= weight
            if (roll < 0) return rarity
        }
        return "common"
    }

    fun getAllCards(): List<Card> = allCards
}
