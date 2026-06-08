package com.example.stepwars2.ui.screens.shop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.data.model.UserChest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ShopItem(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val priceGold: Int = 0,
    val priceGems: Int = 0,
    val category: String // "chest", "boost", "cosmetic"
)

@Composable
fun ShopScreen(
    onOpenChest: (String) -> Unit = {}
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val goldColor = Color(0xFFFFD700)
    val gemColor = Color(0xFFE040FB)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)
    val successGreen = Color(0xFF3FB950)

    val user by UserStateManager.user.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    var myChests by remember { mutableStateOf<List<UserChest>>(emptyList()) }

    // Load user's chests
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        try {
            val snap = firestore.collection("users").document(uid)
                .collection("chests").whereEqualTo("opened", false).get().await()
            myChests = snap.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                UserChest(
                    id = data["id"] as? String ?: doc.id,
                    chestType = data["chestType"] as? String ?: "BRONZE",
                    earnedAt = (data["earnedAt"] as? Number)?.toLong() ?: 0L,
                    opened = false
                )
            }
        } catch (_: Exception) {}
    }

    val chestItems = listOf(
        ShopItem("bronze_chest", "Bronz Sandık", "1 rastgele kart", "📦", priceGold = 50, category = "chest"),
        ShopItem("silver_chest", "Gümüş Sandık", "2 kart, 1 nadir garanti", "🗃️", priceGold = 150, category = "chest"),
        ShopItem("gold_chest", "Altın Sandık", "3 kart, 1 epik garanti", "✨", priceGems = 10, category = "chest"),
        ShopItem("legendary_chest", "Efsanevi Sandık", "4 kart, 1 efsanevi garanti", "🌟", priceGems = 50, category = "chest")
    )

    val boostItems = listOf(
        ShopItem("xp_boost", "XP Takviyesi", "2 saat boyunca 2x XP", "⚡", priceGold = 100, category = "boost"),
        ShopItem("gold_boost", "Altın Takviyesi", "2 saat boyunca 2x altın", "💰", priceGold = 200, category = "boost"),
        ShopItem("step_boost", "Adım Bonusu", "500 bonus adım ekle", "👟", priceGems = 5, category = "boost"),
        ShopItem("shield", "Kupa Kalkanı", "3 savaş boyunca kupa kaybetme", "🛡️", priceGems = 15, category = "boost")
    )

    fun buyItem(item: ShopItem) {
        val currentUser = user ?: return

        val canAfford = if (item.priceGold > 0) {
            currentUser.stepGold >= item.priceGold
        } else {
            currentUser.gems >= item.priceGems
        }

        if (!canAfford) {
            scope.launch {
                snackbarHostState.showSnackbar("Yeterli bakiyen yok!")
            }
            return
        }

        scope.launch {
            try {
                val updates = if (item.priceGold > 0) {
                    mapOf("stepGold" to (currentUser.stepGold - item.priceGold))
                } else {
                    mapOf("gems" to (currentUser.gems - item.priceGems))
                }
                UserStateManager.updateUser(updates)

                // If chest purchase, save to inventory
                if (item.category == "chest") {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        val chestTypeMap = mapOf(
                            "bronze_chest" to "BRONZE",
                            "silver_chest" to "SILVER",
                            "gold_chest" to "GOLD",
                            "legendary_chest" to "LEGENDARY"
                        )
                        val chestType = chestTypeMap[item.id] ?: "BRONZE"
                        val chestId = firestore.collection("users")
                            .document(uid).collection("chests").document().id
                        val chest = UserChest(
                            id = chestId,
                            chestType = chestType,
                            earnedAt = System.currentTimeMillis()
                        )
                        firestore.collection("users").document(uid)
                            .collection("chests").document(chestId)
                            .set(chest.toMap()).await()
                        myChests = myChests + chest
                    }
                }

                snackbarHostState.showSnackbar("${item.name} satın alındı! 🎉")
            } catch (_: Exception) {
                snackbarHostState.showSnackbar("Satın alma başarısız oldu")
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shop_bg")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "orb"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(gemColor.copy(alpha = 0.1f), Color.Transparent),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(size.width * 0.8f, 150f + orbOffset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(goldColor.copy(alpha = 0.08f), Color.Transparent),
                    radius = 180f
                ),
                radius = 180f,
                center = Offset(100f, size.height * 0.5f - orbOffset)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mağaza",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier
                                .background(surfaceDark, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Star, null, tint = goldColor, modifier = Modifier.size(16.dp))
                            Text("${user?.stepGold ?: 0}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = goldColor)
                        }
                        Row(
                            modifier = Modifier
                                .background(surfaceDark, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.Diamond, null, tint = gemColor, modifier = Modifier.size(16.dp))
                            Text("${user?.gems ?: 0}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = gemColor)
                        }
                    }
                }
            }

            // Chests section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("📦 Sandıklar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }

            items(chestItems) { item ->
                ShopItemCard(
                    item = item,
                    surfaceDark = surfaceDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    goldColor = goldColor,
                    gemColor = gemColor,
                    primaryPurple = primaryPurple,
                    onBuy = { buyItem(item) }
                )
            }

            // Boosts section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("⚡ Takviyeler", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }

            items(boostItems) { item ->
                ShopItemCard(
                    item = item,
                    surfaceDark = surfaceDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    goldColor = goldColor,
                    gemColor = gemColor,
                    primaryPurple = primaryPurple,
                    onBuy = { buyItem(item) }
                )
            }

            // My Chests inventory
            if (myChests.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("🎁 Sandıklarım (${myChests.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }

                items(myChests) { chest ->
                    val type = chest.getType()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Mark as opened and navigate
                                scope.launch {
                                    try {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                                        firestore.collection("users").document(uid)
                                            .collection("chests").document(chest.id)
                                            .update("opened", true).await()
                                        myChests = myChests.filter { it.id != chest.id }
                                        onOpenChest(chest.chestType)
                                    } catch (_: Exception) {}
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.emoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(type.displayName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("${type.cardCount} kart + ${type.goldBonus} altın", fontSize = 12.sp, color = textSecondary)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(successGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("AÇ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = successGreen)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ShopItemCard(
    item: ShopItem,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    goldColor: Color,
    gemColor: Color,
    primaryPurple: Color,
    onBuy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(primaryPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    item.description,
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            // Price button
            val isGold = item.priceGold > 0
            val price = if (isGold) item.priceGold else item.priceGems
            val priceColor = if (isGold) goldColor else gemColor

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(priceColor.copy(alpha = 0.15f))
                    .border(1.dp, priceColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .clickable { onBuy() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        if (isGold) Icons.Filled.Star else Icons.Filled.Diamond,
                        contentDescription = null,
                        tint = priceColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "$price",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = priceColor
                    )
                }
            }
        }
    }
}
