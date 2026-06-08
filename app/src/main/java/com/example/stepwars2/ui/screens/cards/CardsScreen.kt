package com.example.stepwars2.ui.screens.cards

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.ui.viewmodel.CardWithDetails
import com.example.stepwars2.ui.viewmodel.CardsViewModel

@Composable
fun CardsScreen(
    viewModel: CardsViewModel = viewModel()
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)

    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val deckCards by viewModel.deckCards.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val userGold by viewModel.userGold.collectAsStateWithLifecycle()
    val upgradeMessage by viewModel.upgradeMessage.collectAsStateWithLifecycle()

    var selectedCard by remember { mutableStateOf<CardWithDetails?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val goldColor = Color(0xFFFFD700)

    // Her ekrana gelişte kartları yeniden yükle
    LaunchedEffect(Unit) {
        viewModel.loadCards()
    }

    LaunchedEffect(upgradeMessage) {
        upgradeMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUpgradeMessage()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cards_bg")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 15f,
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
        // Background orbs
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.4f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryPurple.copy(alpha = 0.12f), Color.Transparent),
                    radius = 250f
                ),
                radius = 250f,
                center = Offset(size.width * 0.85f, 200f + orbOffset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(turquoise.copy(alpha = 0.08f), Color.Transparent),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(80f, size.height * 0.7f - orbOffset)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryPurple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            ) {
                // Title
                Text(
                    text = "Kart Destesi",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Text(
                    text = "${deckCards.size}/4 kart destede",
                    fontSize = 13.sp,
                    color = textSecondary
                )

                // Gold display
                Row(
                    modifier = Modifier
                        .background(surfaceDark, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Star, null, tint = goldColor, modifier = Modifier.size(14.dp))
                    Text("$userGold", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = goldColor)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Active deck
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceDark, RoundedCornerShape(16.dp))
                        .border(1.dp, primaryPurple.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (deckCards.isEmpty()) {
                        Text(
                            text = "Deste boş — kartlar yükleniyor...",
                            color = textSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        deckCards.take(4).forEach { cardDetail ->
                            MiniCardSlot(
                                cardDetail = cardDetail,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // All cards title
                Text(
                    text = "Tüm Kartlar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Cards grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cards) { cardDetail ->
                        FullCardView(
                            cardDetail = cardDetail,
                            viewModel = viewModel,
                            onClick = { selectedCard = cardDetail }
                        )
                    }
                }
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )

        // Upgrade dialog
        selectedCard?.let { card ->
            UpgradeDialog(
                cardDetail = card,
                viewModel = viewModel,
                userGold = userGold,
                onDismiss = { selectedCard = null },
                onUpgrade = {
                    viewModel.upgradeCard(card)
                    selectedCard = null
                },
                onBuyEnergy = {
                    viewModel.buyEnergy(card)
                    selectedCard = null
                },
                onToggleDeck = {
                    viewModel.toggleDeck(card)
                    selectedCard = null
                }
            )
        }
    }
}

@Composable
private fun MiniCardSlot(
    cardDetail: CardWithDetails,
    viewModel: CardsViewModel
) {
    val rarityColor = Color(viewModel.getRarityColor(cardDetail.card.rarity))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            rarityColor.copy(alpha = 0.2f),
                            Color(0xFF161B22)
                        )
                    )
                )
                .border(1.5.dp, rarityColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cardDetail.card.emoji,
                fontSize = 28.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Lv.${cardDetail.userCard.level}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = rarityColor
        )
    }
}

@Composable
private fun FullCardView(
    cardDetail: CardWithDetails,
    viewModel: CardsViewModel,
    onClick: () -> Unit
) {
    val rarityColor = Color(viewModel.getRarityColor(cardDetail.card.rarity))
    val surfaceDark = Color(0xFF161B22)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, rarityColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rarity badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(rarityColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (cardDetail.card.rarity) {
                            "common" -> "Sıradan"
                            "rare" -> "Nadir"
                            "epic" -> "Epik"
                            "legendary" -> "Efsanevi"
                            else -> "Sıradan"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = rarityColor
                    )
                }
                Text(
                    text = "Lv.${cardDetail.userCard.level}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card emoji
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(rarityColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = cardDetail.card.emoji, fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Card name
            Text(
                text = cardDetail.card.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = viewModel.getTypeEmoji(cardDetail.card.type) + " " + when (cardDetail.card.type) {
                    "tank" -> "Tank"
                    "attacker" -> "Saldırgan"
                    "healer" -> "Şifacı"
                    "support" -> "Destek"
                    else -> "Saldırgan"
                },
                fontSize = 11.sp,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatMini(
                    icon = Icons.Filled.Favorite,
                    value = "${cardDetail.effectiveHp}",
                    color = Color(0xFFFF6B6B)
                )
                StatMini(
                    icon = Icons.Filled.Speed,
                    value = "${cardDetail.effectiveAttack}",
                    color = Color(0xFFFFA726)
                )
                StatMini(
                    icon = Icons.Filled.Shield,
                    value = "${cardDetail.effectiveDefense}",
                    color = Color(0xFF58A6FF)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡ ${cardDetail.userCard.currentEnergy()}/${cardDetail.userCard.maxEnergy}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
private fun StatMini(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun UpgradeDialog(
    cardDetail: CardWithDetails,
    viewModel: CardsViewModel,
    userGold: Int,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit,
    onBuyEnergy: () -> Unit,
    onToggleDeck: () -> Unit
) {
    val rarityColor = Color(viewModel.getRarityColor(cardDetail.card.rarity))
    val primaryPurple = Color(0xFF6C63FF)
    val goldColor = Color(0xFFFFD700)
    val successGreen = Color(0xFF3FB950)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)

    val currentLevel = cardDetail.userCard.level
    val isMaxLevel = currentLevel >= 10
    val cost = viewModel.getUpgradeCost(currentLevel)
    val canAfford = userGold >= cost

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF161B22),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(cardDetail.card.emoji, fontSize = 40.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    cardDetail.card.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Lv.${currentLevel}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = rarityColor
                )
            }
        },
        text = {
            Column {
                // Current stats
                Text("Mevcut İstatistikler", fontSize = 12.sp, color = textSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❤️", fontSize = 14.sp)
                        Text("${cardDetail.effectiveHp}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚔️", fontSize = 14.sp)
                        Text("${cardDetail.effectiveAttack}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA726))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛡️", fontSize = 14.sp)
                        Text("${cardDetail.effectiveDefense}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF58A6FF))
                    }
                }

                if (!isMaxLevel) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Yükseltme Sonrası (Lv.${currentLevel + 1})", fontSize = 12.sp, color = successGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("❤️", fontSize = 14.sp)
                            Text("${cardDetail.effectiveHp + 10}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = successGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚔️", fontSize = 14.sp)
                            Text("${cardDetail.effectiveAttack + 5}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = successGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🛡️", fontSize = 14.sp)
                            Text("${cardDetail.effectiveDefense + 3}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = successGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cost display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF21262D), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Maliyet: ", fontSize = 14.sp, color = textSecondary)
                        Icon(Icons.Filled.Star, null, tint = goldColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$cost",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (canAfford) goldColor else Color(0xFFFF6B6B)
                        )
                        Text(
                            " (Bakiye: $userGold)",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "⭐ Maksimum seviye!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = goldColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Enerji UI — her zaman görünür
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1A2332), Color(0xFF21262D))
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("⚡ Enerji", fontSize = 13.sp, color = textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "${cardDetail.userCard.currentEnergy()} / ${cardDetail.userCard.maxEnergy}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cardDetail.userCard.currentEnergy() > 3) goldColor
                                    else if (cardDetail.userCard.currentEnergy() > 0) Color(0xFFFFA726)
                                    else Color(0xFFFF6B6B)
                        )
                    }

                    Button(
                        onClick = onBuyEnergy,
                        enabled = cardDetail.userCard.currentEnergy() < cardDetail.userCard.maxEnergy && userGold >= 10,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF),
                            disabledContainerColor = Color(0xFF21262D)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+5 ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(50", fontSize = 12.sp)
                        Icon(Icons.Filled.Star, null, tint = goldColor, modifier = Modifier.size(13.dp))
                        Text(")", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Desteye ekle/çıkar butonu
                Button(
                    onClick = onToggleDeck,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cardDetail.userCard.inDeck) Color(0xFFFF6B6B) else Color(0xFF3FB950)
                    )
                ) {
                    Text(
                        if (cardDetail.userCard.inDeck) "Desteden Çıkar" else "Desteye Ekle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                // Yükselt butonu
                if (!isMaxLevel) {
                    Button(
                        onClick = onUpgrade,
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryPurple,
                            disabledContainerColor = Color(0xFF21262D)
                        )
                    ) {
                        Text("Yükselt ⬆️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", color = textSecondary)
            }
        }
    )
}
