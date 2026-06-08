package com.example.stepwars2.ui.screens.leaderboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.ui.viewmodel.LeaderboardEntry
import com.example.stepwars2.ui.viewmodel.LeaderboardViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = viewModel()
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val goldColor = Color(0xFFFFD700)
    val silverColor = Color(0xFFC0C0C0)
    val bronzeColor = Color(0xFFCD7F32)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)

    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val currentUserRank by viewModel.currentUserRank.collectAsStateWithLifecycle()
    val numberFormat = NumberFormat.getInstance(Locale("tr", "TR"))

    val tabs = listOf("🏆 Kupa", "👟 Adım", "⭐ Seviye")

    val infiniteTransition = rememberInfiniteTransition(label = "lb_bg")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
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
                    colors = listOf(goldColor.copy(alpha = 0.1f), Color.Transparent),
                    radius = 250f
                ),
                radius = 250f,
                center = Offset(size.width * 0.5f, 100f + orbOffset)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Title
            Text(
                text = "Sıralama",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = surfaceDark,
                contentColor = primaryPurple,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = primaryPurple
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) primaryPurple else textSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current user rank card
            currentUserRank?.let { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, primaryPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryPurple.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📍", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Senin sıran: #${entry.rank}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryPurple
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            getValueForTab(entry, selectedTab, numberFormat),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryPurple)
                }
            } else if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏆", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Henüz sıralama verisi yok",
                            fontSize = 16.sp,
                            color = textSecondary
                        )
                        Text(
                            "Savaşarak sıralamaya gir!",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries) { entry ->
                        LeaderboardRow(
                            entry = entry,
                            selectedTab = selectedTab,
                            numberFormat = numberFormat,
                            surfaceDark = surfaceDark,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            goldColor = goldColor,
                            silverColor = silverColor,
                            bronzeColor = bronzeColor,
                            primaryPurple = primaryPurple
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    selectedTab: Int,
    numberFormat: NumberFormat,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    goldColor: Color,
    silverColor: Color,
    bronzeColor: Color,
    primaryPurple: Color
) {
    val rankColor = when (entry.rank) {
        1 -> goldColor
        2 -> silverColor
        3 -> bronzeColor
        else -> textSecondary
    }

    val rankEmoji = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
    }

    val bgColor = if (entry.isCurrentUser) primaryPurple.copy(alpha = 0.08f) else surfaceDark
    val borderMod = if (entry.isCurrentUser) {
        Modifier.border(1.dp, primaryPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    } else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderMod),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            if (rankEmoji.isNotEmpty()) {
                Text(rankEmoji, fontSize = 20.sp)
            } else {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "#${entry.rank}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (entry.rank <= 3) rankColor else primaryPurple,
                                if (entry.rank <= 3) rankColor.copy(alpha = 0.6f) else primaryPurple.copy(alpha = 0.6f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.user.username.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.user.username,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (entry.isCurrentUser) primaryPurple else textPrimary
                )
                Text(
                    text = "Lv.${entry.user.level} • ${
                        when (entry.user.league) {
                            "silver" -> "Gümüş"
                            "gold" -> "Altın"
                            "platinum" -> "Platin"
                            "diamond" -> "Elmas"
                            else -> "Bronz"
                        }
                    }",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            // Value
            Text(
                text = getValueForTab(entry, selectedTab, numberFormat),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor
            )
        }
    }
}

private fun getValueForTab(entry: LeaderboardEntry, tab: Int, fmt: NumberFormat): String {
    return when (tab) {
        0 -> "🏆 ${fmt.format(entry.user.trophies)}"
        1 -> "👟 ${fmt.format(entry.user.totalSteps)}"
        2 -> "⭐ ${entry.user.level}"
        else -> ""
    }
}
