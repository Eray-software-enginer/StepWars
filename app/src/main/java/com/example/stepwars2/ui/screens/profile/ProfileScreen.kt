package com.example.stepwars2.ui.screens.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepwars2.data.repository.AuthRepository
import com.example.stepwars2.data.repository.UserStateManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

private data class RankEntry(
    val rank: Int,
    val username: String,
    val level: Int,
    val trophies: Int,
    val isCurrentUser: Boolean
)

@Composable
fun ProfileScreen(
    onSignOut: () -> Unit = {}
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val goldColor = Color(0xFFFFD700)
    val gemColor = Color(0xFFE040FB)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)
    val errorRed = Color(0xFFFF6B6B)

    val authRepository = remember { AuthRepository() }
    val user by UserStateManager.user.collectAsStateWithLifecycle()
    val numberFormat = remember { NumberFormat.getInstance(Locale("tr", "TR")) }

    // Dünya sıralaması state
    var rankEntries by remember { mutableStateOf<List<RankEntry>>(emptyList()) }
    var myRank by remember { mutableIntStateOf(0) }
    var isRankLoading by remember { mutableStateOf(true) }

    // Dünya sıralamasını yükle
    LaunchedEffect(Unit) {
        isRankLoading = true
        try {
            val currentUid = authRepository.currentUser?.uid
            val database = FirebaseDatabase.getInstance("https://stepwars2-f0e9e-default-rtdb.europe-west1.firebasedatabase.app")
            val snapshot = database.reference.child("users")
                .orderByChild("level")
                .limitToLast(50)
                .get()
                .await()

            android.util.Log.d("ProfileRank", "Snapshot children count: ${snapshot.childrenCount}")

            data class TempUser(val uid: String, val username: String, val level: Int, val trophies: Int)
            val users = mutableListOf<TempUser>()

            for (child in snapshot.children) {
                try {
                    val uid = child.child("uid").getValue(String::class.java) ?: child.key ?: ""
                    val username = child.child("username").getValue(String::class.java) ?: ""
                    val level = child.child("level").getValue(Int::class.java) ?: 1
                    val trophies = child.child("trophies").getValue(Int::class.java) ?: 0
                    if (uid.isNotEmpty() && username.isNotEmpty()) {
                        users.add(TempUser(uid, username, level, trophies))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProfileRank", "Parse error for child: ${child.key}", e)
                }
            }

            android.util.Log.d("ProfileRank", "Parsed ${users.size} users")

            // Büyükten küçüğe sırala (Firebase ascending döner)
            users.sortWith(compareByDescending<TempUser> { it.level }.thenByDescending { it.trophies })

            rankEntries = users.mapIndexed { index, u ->
                RankEntry(
                    rank = index + 1,
                    username = u.username,
                    level = u.level,
                    trophies = u.trophies,
                    isCurrentUser = u.uid == currentUid
                )
            }.take(10)

            myRank = users.indexOfFirst { it.uid == currentUid } + 1
        } catch (e: Exception) {
            android.util.Log.e("ProfileRank", "Ranking load failed", e)
            rankEntries = emptyList()
        }
        isRankLoading = false
    }

    // Background animation
    val infiniteTransition = rememberInfiniteTransition(label = "profile_bg")
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 20f,
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
        // Background orbs
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.5f)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryPurple.copy(alpha = 0.15f), Color.Transparent),
                    radius = 250f
                ),
                radius = 250f,
                center = Offset(size.width * 0.2f, 150f + orbOffset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(turquoise.copy(alpha = 0.1f), Color.Transparent),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(size.width * 0.85f, size.height * 0.4f - orbOffset)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(primaryPurple, turquoise))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user?.username?.firstOrNull()?.uppercase() ?: "S"),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.username ?: "Yükleniyor...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Filled.Email,
                    contentDescription = null,
                    tint = textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = user?.email ?: "",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Stats Grid
            Text(
                text = "Savaşçı İstatistikleri",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.TrendingUp,
                    label = "Seviye",
                    value = "${user?.level ?: 1}",
                    color = primaryPurple,
                    bg = surfaceDark
                )
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.EmojiEvents,
                    label = "Kupa",
                    value = "${user?.trophies ?: 0}",
                    color = goldColor,
                    bg = surfaceDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.MilitaryTech,
                    label = "Lig",
                    value = when (user?.league) {
                        "silver" -> "Gümüş"
                        "gold" -> "Altın"
                        "platinum" -> "Platin"
                        "diamond" -> "Elmas"
                        "legendary" -> "Efsanevi"
                        else -> "Bronz"
                    },
                    color = turquoise,
                    bg = surfaceDark
                )
                ProfileStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Star,
                    label = "XP",
                    value = numberFormat.format(user?.xp ?: 0),
                    color = Color(0xFFFFA726),
                    bg = surfaceDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Stats
            Text(
                text = "Adım Bilgileri",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DirectionsWalk,
                            contentDescription = null,
                            tint = primaryPurple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = numberFormat.format(user?.totalSteps ?: 0),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text("Toplam Adım", fontSize = 11.sp, color = textSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = goldColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = numberFormat.format(user?.stepGold ?: 0),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                        Text("Altın", fontSize = 11.sp, color = textSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Diamond,
                            contentDescription = null,
                            tint = gemColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = numberFormat.format(user?.gems ?: 0),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = gemColor
                        )
                        Text("Elmas", fontSize = 11.sp, color = textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ===== DÜNYA SIRALAMASI =====
            Text(
                text = "🌍 Dünya Sıralaması",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            if (myRank > 0) {
                Text(
                    text = "Senin sıran: #$myRank",
                    fontSize = 13.sp,
                    color = turquoise,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark)
            ) {
                if (isRankLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryPurple, modifier = Modifier.size(28.dp))
                    }
                } else if (rankEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sıralama yüklenemedi", color = textSecondary, fontSize = 14.sp)
                    }
                } else {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        rankEntries.forEach { entry ->
                            RankRow(
                                entry = entry,
                                primaryPurple = primaryPurple,
                                goldColor = goldColor,
                                turquoise = turquoise,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Sign Out Button
            Button(
                onClick = {
                    authRepository.signOut()
                    onSignOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(errorRed, Color(0xFFCC3333))
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.ExitToApp,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Text(
                            text = "Çıkış Yap",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RankRow(
    entry: RankEntry,
    primaryPurple: Color,
    goldColor: Color,
    turquoise: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val rankEmoji = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (entry.isCurrentUser) Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(primaryPurple.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .border(1.dp, primaryPurple.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sıra numarası
        if (rankEmoji.isNotEmpty()) {
            Text(
                text = rankEmoji,
                fontSize = 20.sp,
                modifier = Modifier.width(36.dp)
            )
        } else {
            Text(
                text = "#${entry.rank}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                modifier = Modifier.width(36.dp)
            )
        }

        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (entry.isCurrentUser) Brush.linearGradient(listOf(primaryPurple, turquoise))
                    else Brush.linearGradient(listOf(Color(0xFF30363D), Color(0xFF21262D)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.username.firstOrNull()?.uppercase() ?: "?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // İsim ve kupa
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (entry.isCurrentUser) "${entry.username} (Sen)" else entry.username,
                fontSize = 14.sp,
                fontWeight = if (entry.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
                color = if (entry.isCurrentUser) primaryPurple else textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "🏆 ${entry.trophies} kupa",
                fontSize = 11.sp,
                color = textSecondary
            )
        }

        // Seviye badge
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            primaryPurple.copy(alpha = 0.3f),
                            turquoise.copy(alpha = 0.2f)
                        )
                    ),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Lv.${entry.level}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = goldColor
            )
        }
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    bg: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6EDF3),
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF8B949E),
                textAlign = TextAlign.Center
            )
        }
    }
}
