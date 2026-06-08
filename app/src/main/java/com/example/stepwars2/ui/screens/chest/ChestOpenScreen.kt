package com.example.stepwars2.ui.screens.chest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepwars2.data.model.Card
import com.example.stepwars2.data.model.ChestType
import com.example.stepwars2.data.model.UserCard
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.domain.usecase.ChestRewardEngine
import com.example.stepwars2.domain.usecase.ChestReward
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun ChestOpenScreen(
    chestTypeStr: String = "BRONZE",
    onClose: () -> Unit = {}
) {
    val chestType = try { ChestType.valueOf(chestTypeStr) } catch (_: Exception) { ChestType.BRONZE }

    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val goldColor = Color(0xFFFFD700)
    val gemColor = Color(0xFFE040FB)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)

    // State
    var phase by remember { mutableIntStateOf(0) } // 0=chest, 1=opening, 2=cards
    var reward by remember { mutableStateOf<ChestReward?>(null) }
    var revealedCount by remember { mutableIntStateOf(0) }

    // Chest shake animation
    val shakeAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(1f) }

    // Phase transitions
    LaunchedEffect(phase) {
        when (phase) {
            0 -> { /* Waiting for tap */ }
            1 -> {
                // Shake animation
                repeat(6) {
                    shakeAnim.animateTo(10f, tween(50))
                    shakeAnim.animateTo(-10f, tween(50))
                }
                shakeAnim.animateTo(0f, tween(50))
                scaleAnim.animateTo(1.5f, tween(300, easing = FastOutSlowInEasing))
                delay(100)
                // Generate reward
                val chestReward = ChestRewardEngine.openChest(chestType)
                reward = chestReward
                scaleAnim.snapTo(1f)

                // Save cards to Firestore + add gold/gems bonus
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val firestore = FirebaseFirestore.getInstance()
                    val cardsRef = firestore.collection("users").document(uid).collection("cards")

                    for (card in chestReward.cards) {
                        try {
                            val existingDoc = cardsRef.document(card.id).get().await()
                            if (existingDoc.exists()) {
                                // Card exists — increase count
                                val currentCount = (existingDoc.getLong("count") ?: 1L).toInt()
                                cardsRef.document(card.id).update("count", currentCount + 1).await()
                            } else {
                                // New card — add to collection
                                val userCard = UserCard(
                                    id = card.id,
                                    cardId = card.id,
                                    userId = uid,
                                    level = 1,
                                    count = 1,
                                    inDeck = false,
                                    deckPosition = -1
                                )
                                cardsRef.document(card.id).set(userCard.toMap()).await()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChestOpen", "Failed to save card ${card.id}: ${e.message}")
                        }
                    }

                    // Add gold and gems bonus
                    try {
                        val user = UserStateManager.currentUser()
                        if (user != null) {
                            val updates = mutableMapOf<String, Any>()
                            if (chestReward.goldBonus > 0) {
                                updates["stepGold"] = user.stepGold + chestReward.goldBonus
                            }
                            if (chestReward.gemsBonus > 0) {
                                updates["gems"] = user.gems + chestReward.gemsBonus
                            }
                            if (updates.isNotEmpty()) {
                                UserStateManager.updateUser(updates)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChestOpen", "Failed to add bonus: ${e.message}")
                    }
                }
                
                // Advance to phase 2 AFTER saving
                phase = 2
            }
            2 -> {
                // Reveal cards one by one
                val totalCards = reward?.cards?.size ?: 0
                for (i in 0 until totalCards) {
                    delay(600)
                    revealedCount = i + 1
                }
            }
        }
    }

    // Background glow
    val infiniteTransition = rememberInfiniteTransition(label = "chest_bg")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = LinearEasing), RepeatMode.Reverse
        ), label = "glow"
    )

    val chestGlowColor = when (chestType) {
        ChestType.BRONZE -> Color(0xFFCD7F32)
        ChestType.SILVER -> Color(0xFFC0C0C0)
        ChestType.GOLD -> goldColor
        ChestType.EPIC -> primaryPurple
        ChestType.LEGENDARY -> gemColor
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg),
        contentAlignment = Alignment.Center
    ) {
        // Background radial glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        chestGlowColor.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.minDimension * 0.6f
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.minDimension * 0.6f
            )
        }

        when (phase) {
            0 -> {
                // Chest waiting to be opened
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = chestType.displayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = chestGlowColor,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${chestType.cardCount} kart + ${chestType.goldBonus} altın",
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    // Chest emoji - tap to open
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(scaleAnim.value)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        chestGlowColor.copy(alpha = 0.2f),
                                        surfaceDark
                                    )
                                )
                            )
                            .border(2.dp, chestGlowColor.copy(alpha = 0.5f), CircleShape)
                            .clickable { phase = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chestType.emoji,
                            fontSize = 64.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Açmak için dokun!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = chestGlowColor,
                        modifier = Modifier.alpha(glowAlpha)
                    )
                }
            }

            1 -> {
                // Opening animation
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scaleAnim.value)
                        .rotate(shakeAnim.value),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = chestType.emoji, fontSize = 72.sp)
                }
            }

            2 -> {
                // Cards revealed
                val currentReward = reward ?: return

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Title
                    Text(
                        text = "${chestType.displayName} Açıldı!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = chestGlowColor,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Cards carousel
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(currentReward.cards) { index, card ->
                            AnimatedVisibility(
                                visible = index < revealedCount,
                                enter = scaleIn(tween(400)) + fadeIn(tween(400))
                            ) {
                                CardRevealItem(
                                    card = card,
                                    surfaceDark = surfaceDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Gold & Gems bonus
                    if (revealedCount >= currentReward.cards.size) {
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(500))
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (currentReward.goldBonus > 0) {
                                    Text(
                                        text = "💰 +${currentReward.goldBonus} Altın",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = goldColor
                                    )
                                }
                                if (currentReward.gemsBonus > 0) {
                                    Spacer(modifier = Modifier.width(20.dp))
                                    Text(
                                        text = "💎 +${currentReward.gemsBonus} Elmas",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gemColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Close button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primaryPurple, Color(0xFF9C27B0))
                                    )
                                )
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "TAMAM",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardRevealItem(
    card: Card,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val rarityColor = when (card.rarity) {
        "common" -> Color(0xFF8B949E)
        "rare" -> Color(0xFF58A6FF)
        "epic" -> Color(0xFFA855F7)
        "legendary" -> Color(0xFFFFD700)
        else -> Color(0xFF8B949E)
    }

    val rarityName = when (card.rarity) {
        "common" -> "Sıradan"
        "rare" -> "Nadir"
        "epic" -> "Epik"
        "legendary" -> "Efsanevi"
        else -> "Sıradan"
    }

    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(surfaceDark)
            .border(1.5.dp, rarityColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(rarityColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = card.emoji, fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = card.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Rarity badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(rarityColor.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = rarityName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = rarityColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("⚔${card.baseAttack}", fontSize = 10.sp, color = Color(0xFFFFA726))
            Text("🛡${card.baseDefense}", fontSize = 10.sp, color = Color(0xFF58A6FF))
        }
    }
}
