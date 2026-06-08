package com.example.stepwars2.data.model

data class DailyQuest(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val type: QuestType,
    val targetValue: Int,
    val currentValue: Int = 0,
    val rewardGold: Int = 0,
    val rewardGems: Int = 0,
    val rewardXp: Int = 0,
    val isClaimed: Boolean = false
) {
    val isCompleted: Boolean get() = currentValue >= targetValue
    val progress: Float get() = (currentValue.toFloat() / targetValue).coerceAtMost(1f)
}

enum class QuestType {
    WALK_STEPS,     // Belirli sayıda adım at
    WIN_BATTLES,    // Belirli sayıda savaş kazan
    PLAY_BATTLES,   // Belirli sayıda savaş yap
    UPGRADE_CARD,   // Kart yükselt
    OPEN_CHEST,     // Sandık aç
    LOGIN_DAILY     // Günlük giriş
}

object DailyQuestGenerator {

    fun generateDailyQuests(): List<DailyQuest> {
        return listOf(
            DailyQuest(
                id = "daily_login",
                title = "Günlük Giriş",
                description = "Uygulamaya giriş yap",
                emoji = "👋",
                type = QuestType.LOGIN_DAILY,
                targetValue = 1,
                currentValue = 1, // Auto-completed on login
                rewardGold = 10,
                rewardXp = 5
            ),
            DailyQuest(
                id = "walk_1000",
                title = "Sabah Yürüyüşü",
                description = "1.000 adım at",
                emoji = "🚶",
                type = QuestType.WALK_STEPS,
                targetValue = 1000,
                rewardGold = 15,
                rewardXp = 10
            ),
            DailyQuest(
                id = "walk_5000",
                title = "Aktif Savaşçı",
                description = "5.000 adım at",
                emoji = "🏃",
                type = QuestType.WALK_STEPS,
                targetValue = 5000,
                rewardGold = 30,
                rewardGems = 2,
                rewardXp = 20
            ),
            DailyQuest(
                id = "walk_10000",
                title = "Maraton Koşucusu",
                description = "10.000 adım at",
                emoji = "🏅",
                type = QuestType.WALK_STEPS,
                targetValue = 10000,
                rewardGold = 50,
                rewardGems = 5,
                rewardXp = 40
            ),
            DailyQuest(
                id = "battle_1",
                title = "Arena Savaşçısı",
                description = "1 savaş yap",
                emoji = "⚔️",
                type = QuestType.PLAY_BATTLES,
                targetValue = 1,
                rewardGold = 15,
                rewardXp = 10
            ),
            DailyQuest(
                id = "win_1",
                title = "Zafer Yolu",
                description = "1 savaş kazan",
                emoji = "🏆",
                type = QuestType.WIN_BATTLES,
                targetValue = 1,
                rewardGold = 25,
                rewardGems = 1,
                rewardXp = 15
            )
        )
    }
}
