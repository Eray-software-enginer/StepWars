package com.example.stepwars2.data.model

data class Card(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val rarity: String = "common", // common, rare, epic, legendary
    val type: String = "attacker", // tank, attacker, healer, support
    val baseHp: Int = 100,
    val baseAttack: Int = 20,
    val baseDefense: Int = 10,
    val baseSpeed: Int = 5,
    val imageUrl: String = "",
    val emoji: String = "⚔️"
) {
    constructor() : this(id = "")

    companion object {
        fun getStarterCards(): List<Card> = listOf(
            Card(
                id = "warrior_01", name = "Savaşçı",
                description = "Güçlü yakın dövüş savaşçısı. Dengeli saldırı ve savunma.",
                rarity = "common", type = "attacker",
                baseHp = 120, baseAttack = 25, baseDefense = 15, baseSpeed = 5,
                emoji = "⚔️"
            ),
            Card(
                id = "archer_01", name = "Okçu",
                description = "Uzak mesafeden saldırır. Yüksek hasar ama düşük can.",
                rarity = "common", type = "attacker",
                baseHp = 80, baseAttack = 35, baseDefense = 8, baseSpeed = 7,
                emoji = "🏹"
            ),
            Card(
                id = "shield_01", name = "Kalkan Muhafızı",
                description = "Yüksek savunma. Takım arkadaşlarını korur.",
                rarity = "common", type = "tank",
                baseHp = 200, baseAttack = 10, baseDefense = 30, baseSpeed = 3,
                emoji = "🛡️"
            ),
            Card(
                id = "healer_01", name = "Şifacı",
                description = "Takım arkadaşlarının canını yeniler.",
                rarity = "common", type = "healer",
                baseHp = 90, baseAttack = 12, baseDefense = 12, baseSpeed = 6,
                emoji = "💚"
            )
        )

        /**
         * Oyundaki tüm kartlar — 17 kart
         * Common (6), Rare (5), Epic (4), Legendary (3)
         */
        fun getAllCards(): List<Card> = listOf(
            // ═══════════════════ COMMON (6) ═══════════════════
            Card(
                id = "warrior_01", name = "Savaşçı",
                description = "Güçlü yakın dövüş savaşçısı. Dengeli saldırı ve savunma.",
                rarity = "common", type = "attacker",
                baseHp = 120, baseAttack = 25, baseDefense = 15, baseSpeed = 5,
                emoji = "⚔️"
            ),
            Card(
                id = "archer_01", name = "Okçu",
                description = "Uzak mesafeden saldırır. Yüksek hasar ama düşük can.",
                rarity = "common", type = "attacker",
                baseHp = 80, baseAttack = 35, baseDefense = 8, baseSpeed = 7,
                emoji = "🏹"
            ),
            Card(
                id = "shield_01", name = "Kalkan Muhafızı",
                description = "Yüksek savunma. Takım arkadaşlarını korur.",
                rarity = "common", type = "tank",
                baseHp = 200, baseAttack = 10, baseDefense = 30, baseSpeed = 3,
                emoji = "🛡️"
            ),
            Card(
                id = "healer_01", name = "Şifacı",
                description = "Takım arkadaşlarının canını yeniler.",
                rarity = "common", type = "healer",
                baseHp = 90, baseAttack = 12, baseDefense = 12, baseSpeed = 6,
                emoji = "💚"
            ),
            Card(
                id = "knight_01", name = "Şövalye",
                description = "Zırhıyla korunan dengeli tank savaşçı.",
                rarity = "common", type = "tank",
                baseHp = 150, baseAttack = 20, baseDefense = 25, baseSpeed = 4,
                emoji = "🗡️"
            ),
            Card(
                id = "scout_01", name = "Keşifçi",
                description = "Hızlı ve çevik. İlk vuruşta avantajlı.",
                rarity = "common", type = "attacker",
                baseHp = 70, baseAttack = 28, baseDefense = 10, baseSpeed = 9,
                emoji = "🏃"
            ),

            // ═══════════════════ RARE (5) ═══════════════════
            Card(
                id = "mage_01", name = "Büyücü",
                description = "Güçlü büyü saldırıları. Savunmayı delip geçer.",
                rarity = "rare", type = "attacker",
                baseHp = 100, baseAttack = 45, baseDefense = 10, baseSpeed = 6,
                emoji = "🧙"
            ),
            Card(
                id = "paladin_01", name = "Paladin",
                description = "Kutsal zırh taşır. Hem tank hem destek.",
                rarity = "rare", type = "tank",
                baseHp = 180, baseAttack = 22, baseDefense = 28, baseSpeed = 4,
                emoji = "⚜️"
            ),
            Card(
                id = "ninja_01", name = "Ninja",
                description = "Gölgelerden saldırır. Kritik vuruş şansı yüksek.",
                rarity = "rare", type = "attacker",
                baseHp = 85, baseAttack = 40, baseDefense = 12, baseSpeed = 10,
                emoji = "🥷"
            ),
            Card(
                id = "cleric_01", name = "Rahip",
                description = "Güçlü şifa büyüleri. Takımı ayakta tutar.",
                rarity = "rare", type = "healer",
                baseHp = 110, baseAttack = 15, baseDefense = 20, baseSpeed = 5,
                emoji = "✝️"
            ),
            Card(
                id = "berserker_01", name = "Berserker",
                description = "Canı azaldıkça saldırısı artar. Öfke gücü.",
                rarity = "rare", type = "attacker",
                baseHp = 130, baseAttack = 38, baseDefense = 8, baseSpeed = 7,
                emoji = "🪓"
            ),

            // ═══════════════════ EPIC (4) ═══════════════════
            Card(
                id = "dragon_01", name = "Ejderha",
                description = "Ateş nefesi ile tüm düşmanları yakar.",
                rarity = "epic", type = "attacker",
                baseHp = 200, baseAttack = 55, baseDefense = 20, baseSpeed = 6,
                emoji = "🐉"
            ),
            Card(
                id = "golem_01", name = "Golem",
                description = "Yıkılmaz kaya devri. Devasa dayanıklılık.",
                rarity = "epic", type = "tank",
                baseHp = 350, baseAttack = 15, baseDefense = 40, baseSpeed = 2,
                emoji = "🗿"
            ),
            Card(
                id = "phoenix_01", name = "Anka Kuşu",
                description = "Küllerinden yeniden doğar. Can yenileme.",
                rarity = "epic", type = "healer",
                baseHp = 150, baseAttack = 35, baseDefense = 15, baseSpeed = 8,
                emoji = "🔥"
            ),
            Card(
                id = "assassin_01", name = "Suikastçı",
                description = "Kritik vuruş ustası. Tek hamlede öldürebilir.",
                rarity = "epic", type = "attacker",
                baseHp = 100, baseAttack = 65, baseDefense = 5, baseSpeed = 12,
                emoji = "🗡️"
            ),

            // ═══════════════════ LEGENDARY (3) ═══════════════════
            Card(
                id = "zeus_01", name = "Zeus",
                description = "Yıldırımlarla yok eder. Tanrıların kralı.",
                rarity = "legendary", type = "attacker",
                baseHp = 250, baseAttack = 70, baseDefense = 25, baseSpeed = 8,
                emoji = "⚡"
            ),
            Card(
                id = "titan_01", name = "Titan",
                description = "Devasa güç ve dayanıklılık. Durdurulamaz.",
                rarity = "legendary", type = "tank",
                baseHp = 500, baseAttack = 30, baseDefense = 50, baseSpeed = 3,
                emoji = "👹"
            ),
            Card(
                id = "angel_01", name = "Melek",
                description = "İlahi güçle iyileştirir. Takımı ölümsüz kılar.",
                rarity = "legendary", type = "healer",
                baseHp = 200, baseAttack = 40, baseDefense = 30, baseSpeed = 10,
                emoji = "👼"
            )
        )

        /** ID'ye göre kart bul */
        fun findById(id: String): Card? = getAllCards().find { it.id == id }

        /** Tüm kartları Map olarak al (hızlı erişim) */
        fun allCardsMap(): Map<String, Card> = getAllCards().associateBy { it.id }
    }
}
