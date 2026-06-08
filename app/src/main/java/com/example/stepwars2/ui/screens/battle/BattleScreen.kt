package com.example.stepwars2.ui.screens.battle

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.ui.viewmodel.BattleCard
import com.example.stepwars2.ui.viewmodel.BattleState
import com.example.stepwars2.ui.viewmodel.BattleViewModel
import com.example.stepwars2.data.service.OnlineBattleService
import androidx.compose.runtime.DisposableEffect

@Composable
fun BattleScreen(
    viewModel: BattleViewModel = viewModel()
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)
    val goldColor = Color(0xFFFFD700)
    val errorRed = Color(0xFFFF6B6B)
    val successGreen = Color(0xFF3FB950)

    val battleState by viewModel.battleState.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "battle")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // ✅ Ekrandan çıkılınca online savaşı anlık bitir
    DisposableEffect(Unit) {
        onDispose {
            val state = viewModel.battleState.value
            if (state is BattleState.InBattle && state.isOnline && state.battleId.isNotEmpty()) {
                val myKey = if (state.isPlayer1) "player1" else "player2"
                val opKey = if (state.isPlayer1) "player2" else "player1"
                OnlineBattleService.surrenderBattle(state.battleId, myKey, opKey)
                viewModel.resetBattle()
            } else if (state is BattleState.Searching) {
                viewModel.cancelSearch()
            }
        }
    }

    // ✅ Savaş sırasında geri tuşunu engelle
    val isInActiveBattle = battleState is BattleState.InBattle || battleState is BattleState.Searching
    BackHandler(enabled = isInActiveBattle) {
        // Geri tuşunu yut — savaştan kaçış yok!
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        when (val state = battleState) {
            is BattleState.Idle -> {
                IdleScreen(
                    primaryPurple = primaryPurple,
                    turquoise = turquoise,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    pulseScale = pulseScale,
                    onStartBattle = { viewModel.startBattle() }
                )
            }

            is BattleState.Searching -> {
                SearchingScreen(
                    state = state,
                    primaryPurple = primaryPurple,
                    turquoise = turquoise,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    errorRed = errorRed,
                    onCancel = { viewModel.cancelSearch() }
                )
            }

            is BattleState.InBattle -> {
                InBattleScreen(
                    state = state,
                    surfaceDark = surfaceDark,
                    primaryPurple = primaryPurple,
                    turquoise = turquoise,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    errorRed = errorRed,
                    successGreen = successGreen,
                    onAttack = { viewModel.attack() }
                )
            }

            is BattleState.Finished -> {
                FinishedScreen(
                    state = state,
                    primaryPurple = primaryPurple,
                    turquoise = turquoise,
                    goldColor = goldColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    successGreen = successGreen,
                    errorRed = errorRed,
                    onPlayAgain = { viewModel.resetBattle() }
                )
            }

            is BattleState.InsufficientGold -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💰", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Yetersiz Altın!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = errorRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Savaşa girmek için ${state.required} altın gerekli.\nMevcut: ${state.current} altın",
                        fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetBattle() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Tamam", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            is BattleState.NoEnergy -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("⚡", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Enerji Yok!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = errorRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${state.cardName} kartının enerjisi bitti.\nEnerji zamanla yenilenir veya altınla satın alabilirsin.",
                        fontSize = 14.sp, color = textSecondary, textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.resetBattle() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Tamam", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleScreen(
    primaryPurple: Color, turquoise: Color,
    textPrimary: Color, textSecondary: Color,
    pulseScale: Float, onStartBattle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("⚔️", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ARENA",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.linearGradient(listOf(primaryPurple, turquoise))
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gerçek rakip bul veya bot ile savaş!",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "60 saniye içinde rakip bulunamazsa bot gelir",
            fontSize = 11.sp,
            color = textSecondary.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(primaryPurple, Color(0xFF9C27B0))))
                .clickable { onStartBattle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SAVAŞ BUL",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun SearchingScreen(
    state: BattleState.Searching,
    primaryPurple: Color, turquoise: Color,
    textPrimary: Color, textSecondary: Color,
    errorRed: Color,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "searchPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Geri sayım dairesi
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { state.secondsRemaining / 60f },
                color = if (state.secondsRemaining > 15) primaryPurple else errorRed,
                trackColor = Color(0xFF21262D),
                modifier = Modifier.size(120.dp),
                strokeWidth = 6.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${state.secondsRemaining}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.secondsRemaining > 15) textPrimary else errorRed
                )
                Text(
                    text = "saniye",
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Rakip Aranıyor...",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.then(
                Modifier // pulse effect via alpha
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kupa aralığında uygun rakip bekleniyor",
            fontSize = 13.sp,
            color = textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (state.secondsRemaining <= 15) "⏳ Bot hazırlanıyor..." else "🌐 Online eşleşme",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (state.secondsRemaining <= 15) Color(0xFFFFA726) else turquoise
        )

        Spacer(modifier = Modifier.height(40.dp))

        // İptal butonu
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF21262D))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "İPTAL",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = errorRed,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun InBattleScreen(
    state: BattleState.InBattle,
    surfaceDark: Color, primaryPurple: Color, turquoise: Color,
    textPrimary: Color, textSecondary: Color,
    errorRed: Color, successGreen: Color,
    onAttack: () -> Unit
) {
    val playerCard = state.playerCards.getOrNull(state.playerActiveIndex)
    val enemyCard = state.enemyCards.getOrNull(state.enemyActiveIndex)

    if (playerCard == null || enemyCard == null) {
        // Online savaş yükleniyor
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = primaryPurple)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Savaş yükleniyor...", color = textPrimary)
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.battleLog.size) {
        if (state.battleLog.isNotEmpty()) {
            listState.animateScrollToItem(state.battleLog.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header — Online/Bot göstergesi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Online badge
            Row(
                modifier = Modifier
                    .background(
                        if (state.isOnline) successGreen.copy(alpha = 0.15f)
                        else Color(0xFFFFA726).copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (state.isOnline) "🌐" else "🤖",
                    fontSize = 12.sp
                )
                Text(
                    text = if (state.isOnline) "ONLINE" else "BOT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isOnline) successGreen else Color(0xFFFFA726)
                )
            }

            Text(
                state.enemyName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = errorRed
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Enemy card
        BattleCardView(
            card = enemyCard,
            label = state.enemyName,
            isEnemy = true,
            surfaceDark = surfaceDark,
            textPrimary = textPrimary,
            errorRed = errorRed,
            successGreen = successGreen,
            primaryPurple = primaryPurple
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Battle log
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceDark)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.battleLog) { log ->
                    Text(
                        text = log.message,
                        fontSize = 12.sp,
                        color = if (log.isPlayerAction) successGreen else errorRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Player card
        BattleCardView(
            card = playerCard,
            label = "Sen",
            isEnemy = false,
            surfaceDark = surfaceDark,
            textPrimary = textPrimary,
            errorRed = errorRed,
            successGreen = successGreen,
            primaryPurple = primaryPurple
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Attack button with turn timer
        val timerColor = when {
            state.turnTimeRemaining <= 5 -> errorRed
            state.turnTimeRemaining <= 10 -> Color(0xFFFFA726)
            else -> primaryPurple
        }

        if (state.isPlayerTurn) {
            // Timer bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF21262D))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(state.turnTimeRemaining / 15f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(timerColor)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Button(
            onClick = onAttack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = state.isPlayerTurn,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isPlayerTurn) timerColor else surfaceDark,
                disabledContainerColor = surfaceDark
            )
        ) {
            if (state.isPlayerTurn) {
                Text(
                    text = "⚔️ SALDIRI!  (${state.turnTimeRemaining}s)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "⏳ Rakip oynuyor...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary.copy(alpha = 0.5f)
                )
            }
        }

        // Player remaining cards
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.playerCards.forEachIndexed { index, card ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (card.currentHp > 0) {
                                if (index == state.playerActiveIndex) primaryPurple
                                else Color(0xFF21262D)
                            } else errorRed.copy(alpha = 0.3f)
                        )
                        .then(
                            if (index == state.playerActiveIndex)
                                Modifier.border(1.5.dp, primaryPurple, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        card.emoji,
                        fontSize = 14.sp,
                        modifier = if (card.currentHp <= 0) Modifier else Modifier
                    )
                }
                if (index < state.playerCards.size - 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}

@Composable
private fun BattleCardView(
    card: BattleCard,
    label: String,
    isEnemy: Boolean,
    surfaceDark: Color,
    textPrimary: Color,
    errorRed: Color,
    successGreen: Color,
    primaryPurple: Color
) {
    val hpPercent = card.currentHp.toFloat() / card.maxHp.toFloat()
    val hpColor = when {
        hpPercent > 0.5f -> successGreen
        hpPercent > 0.25f -> Color(0xFFFFA726)
        else -> errorRed
    }
    val borderColor = if (isEnemy) errorRed.copy(alpha = 0.4f) else primaryPurple.copy(alpha = 0.4f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isEnemy) errorRed.copy(alpha = 0.1f)
                        else primaryPurple.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(card.emoji, fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        card.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        "Lv.${card.level}",
                        fontSize = 12.sp,
                        color = if (isEnemy) errorRed else primaryPurple
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // HP bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "❤️ ${card.currentHp}/${card.maxHp}",
                        fontSize = 11.sp,
                        color = hpColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF21262D))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hpPercent)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(hpColor)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("⚔️${card.attack}", fontSize = 10.sp, color = Color(0xFFFFA726))
                    Text("🛡️${card.defense}", fontSize = 10.sp, color = Color(0xFF58A6FF))
                    Text("💨${card.speed}", fontSize = 10.sp, color = Color(0xFF3FB950))
                }
            }
        }
    }
}

@Composable
private fun FinishedScreen(
    state: BattleState.Finished,
    primaryPurple: Color, turquoise: Color,
    goldColor: Color, textPrimary: Color, textSecondary: Color,
    successGreen: Color, errorRed: Color,
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(tween(500)) + fadeIn(tween(500))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (state.won) "🏆" else "💀",
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (state.won) "ZAFER!" else "YENİLGİ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.won) goldColor else errorRed,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Online/Bot göstergesi
                Row(
                    modifier = Modifier
                        .background(
                            if (state.isOnline) successGreen.copy(alpha = 0.1f)
                            else Color(0xFFFFA726).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (state.isOnline) "🌐 Online Savaş" else "🤖 Bot Savaşı",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.isOnline) successGreen else Color(0xFFFFA726)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.enemyName} ile savaş sona erdi",
                    fontSize = 14.sp,
                    color = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Rewards card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ödüller",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (state.trophyChange >= 0) successGreen else errorRed,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "${if (state.trophyChange >= 0) "+" else ""}${state.trophyChange}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.trophyChange >= 0) successGreen else errorRed
                        )
                        Text("Kupa", fontSize = 11.sp, color = textSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "+${state.goldReward}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                        Text("Altın", fontSize = 11.sp, color = textSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(primaryPurple, turquoise)))
                .clickable { onPlayAgain() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TEKRAR OYNA",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}
