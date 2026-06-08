package com.example.stepwars2.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.ui.viewmodel.HomeViewModel
import com.example.stepwars2.ui.viewmodel.QuestViewModel
import com.example.stepwars2.data.model.DailyQuest
import java.text.NumberFormat
import java.util.Locale



data class Milestone(val steps: Int, val reward: String, val completed: Boolean)

@Composable
fun HomeScreen(
    onNavigateToBattle: () -> Unit = {},
    onNavigateToShop: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
    questViewModel: QuestViewModel = viewModel()
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

    val user by viewModel.user.collectAsStateWithLifecycle()
    val todaySteps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val homeMessage by viewModel.message.collectAsStateWithLifecycle()
    val questMessage by questViewModel.claimMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar messages
    LaunchedEffect(homeMessage) {
        homeMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(questMessage) {
        questMessage?.let {
            snackbarHostState.showSnackbar(it)
            questViewModel.clearClaimMessage()
        }
    }

    val numberFormat = remember { NumberFormat.getInstance(Locale("tr", "TR")) }

    // Permission handling
    var permissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val activityGranted = permissions[Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true
        if (activityGranted) {
            permissionGranted = true
            viewModel.startStepCounter()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    // Step progress
    val dailyGoal = viewModel.dailyGoal
    val targetProgress = if (dailyGoal > 0) (todaySteps.toFloat() / dailyGoal).coerceAtMost(1f) else 0f

    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val progressAnimation by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LaunchedEffect(targetProgress) {
        animatedProgress = targetProgress
    }

    // Pulsing battle button
    val infiniteTransition = rememberInfiniteTransition(label = "home_pulse")
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button_pulse"
    )

    // Floating orbs
    val orbOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb"
    )

    // Milestones
    val milestones = listOf(
        Milestone(1000, "10 Altın", todaySteps >= 1000),
        Milestone(3000, "Bronz Sandık", todaySteps >= 3000),
        Milestone(5000, "25 Altın", todaySteps >= 5000),
        Milestone(7500, "Gümüş Sandık", todaySteps >= 7500),
        Milestone(10000, "50 Altın + Altın Sandık", todaySteps >= 10000)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        // Background orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryPurple.copy(alpha = 0.08f), Color.Transparent),
                    radius = 300f
                ),
                radius = 300f,
                center = Offset(size.width * 0.8f, 100f + orbOffset)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(turquoise.copy(alpha = 0.06f), Color.Transparent),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(100f, size.height * 0.6f - orbOffset)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Greeting & currencies row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Merhaba, ${user?.username ?: "Savaşçı"}! 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Bugün harika bir gün!",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CurrencyChip(
                        icon = Icons.Filled.Star,
                        value = numberFormat.format(user?.stepGold ?: 0),
                        color = goldColor
                    )
                    CurrencyChip(
                        icon = Icons.Filled.Diamond,
                        value = numberFormat.format(user?.gems ?: 0),
                        color = gemColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Progress Ring
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryPurple.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        radius = size.minDimension / 2
                    )
                }

                // Background ring
                Canvas(modifier = Modifier.size(190.dp)) {
                    drawArc(
                        color = surfaceDark,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 14f, cap = StrokeCap.Round),
                        topLeft = Offset(7f, 7f),
                        size = Size(size.width - 14f, size.height - 14f)
                    )
                }

                // Progress ring
                Canvas(modifier = Modifier.size(190.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(primaryPurple, turquoise, primaryPurple)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * progressAnimation,
                        useCenter = false,
                        style = Stroke(width = 14f, cap = StrokeCap.Round),
                        topLeft = Offset(7f, 7f),
                        size = Size(size.width - 14f, size.height - 14f)
                    )
                }

                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.DirectionsWalk,
                        contentDescription = null,
                        tint = primaryPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = numberFormat.format(todaySteps),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Text(
                        text = "/ ${numberFormat.format(dailyGoal)} adım",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(
                    icon = Icons.Filled.EmojiEvents,
                    label = "Kupa",
                    value = "${user?.trophies ?: 0}",
                    color = goldColor
                )
                StatBadge(
                    icon = Icons.Filled.MilitaryTech,
                    label = "Lig",
                    value = viewModel.getLeagueName(user?.league ?: "bronze"),
                    color = turquoise
                )
                StatBadge(
                    icon = Icons.Filled.Star,
                    label = "Seviye",
                    value = "${user?.level ?: 1}",
                    color = primaryPurple
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ⭐ Level Progress Bar
            val currentLevel = user?.level ?: 1
            val currentXp = user?.xp ?: 0
            val xpForNextLevel = currentLevel * 100 // Her seviye için 100 * level XP
            val xpProgress = (currentXp.toFloat() / xpForNextLevel).coerceIn(0f, 1f)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(primaryPurple, turquoise))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$currentLevel",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Seviye $currentLevel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Text(
                            text = "$currentXp / $xpForNextLevel XP",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF21262D))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(xpProgress)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(primaryPurple, turquoise))
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sonraki seviyeye ${xpForNextLevel - currentXp} XP kaldı",
                        fontSize = 11.sp,
                        color = textSecondary.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Battle Button
            Box(
                modifier = Modifier
                    .scale(buttonScale)
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = primaryPurple.copy(alpha = 0.4f),
                        spotColor = primaryPurple.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(primaryPurple, Color(0xFF9C27B0))
                        )
                    )
                    .clickable { onNavigateToBattle() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("⚔️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SAVAŞA GİR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Conversion Button
            val unconvertedSteps = user?.unconvertedSteps ?: 0
            val potentialGold = unconvertedSteps / viewModel.stepsPerGold

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🔄 Adım Dönüştürme",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "$unconvertedSteps adım → $potentialGold altın (100 adım = 1 ⭐)",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (potentialGold > 0)
                                    Brush.horizontalGradient(listOf(goldColor, Color(0xFFFFA726)))
                                else Brush.linearGradient(listOf(Color(0xFF21262D), Color(0xFF21262D)))
                            )
                            .clickable(enabled = potentialGold > 0) {
                                viewModel.convertStepsToGold()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (potentialGold > 0) "DÖNÜŞTÜR (+$potentialGold ⭐)" else "Yeterli adım yok",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (potentialGold > 0) Color(0xFF0D1117) else textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Milestones section
            Text(
                text = "Günlük Hedefler",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            milestones.forEach { milestone ->
                MilestoneCard(
                    milestone = milestone,
                    todaySteps = todaySteps,
                    surfaceDark = surfaceDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    successGreen = successGreen,
                    primaryPurple = primaryPurple,
                    goldColor = goldColor,
                    numberFormat = numberFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Daily Quests Section
            DailyQuestsSection(
                questViewModel = questViewModel,
                surfaceDark = surfaceDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                primaryPurple = primaryPurple,
                successGreen = successGreen,
                goldColor = goldColor
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun CurrencyChip(
    icon: ImageVector,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .background(
                Color(0xFF161B22).copy(alpha = 0.8f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StatBadge(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF161B22), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6EDF3)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF8B949E)
        )
    }
}

@Composable
private fun MilestoneCard(
    milestone: Milestone,
    todaySteps: Int,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    successGreen: Color,
    primaryPurple: Color,
    goldColor: Color,
    numberFormat: NumberFormat
) {
    val progress = (todaySteps.toFloat() / milestone.steps).coerceAtMost(1f)

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
            // Status icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (milestone.completed) successGreen.copy(alpha = 0.15f)
                        else surfaceDark,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (milestone.completed) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = if (milestone.completed) successGreen else textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${numberFormat.format(milestone.steps)} adım",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (milestone.completed) successGreen else textPrimary
                )
                Text(
                    text = milestone.reward,
                    fontSize = 12.sp,
                    color = if (milestone.completed) successGreen.copy(alpha = 0.7f) else goldColor
                )

                // Progress bar
                if (!milestone.completed) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF21262D))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(primaryPurple, Color(0xFF00D2FF))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestsSection(
    questViewModel: QuestViewModel,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryPurple: Color,
    successGreen: Color,
    goldColor: Color
) {
    val quests by questViewModel.quests.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 Günlük Görevler",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = "${quests.count { it.isCompleted }}/${quests.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryPurple
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        quests.forEach { quest ->
            QuestCard(
                quest = quest,
                surfaceDark = surfaceDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                primaryPurple = primaryPurple,
                successGreen = successGreen,
                goldColor = goldColor,
                onClaim = { questViewModel.claimReward(quest.id) }
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun QuestCard(
    quest: DailyQuest,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryPurple: Color,
    successGreen: Color,
    goldColor: Color,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (quest.isClaimed) surfaceDark.copy(alpha = 0.5f) else surfaceDark
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Text(quest.emoji, fontSize = 24.sp)

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    quest.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (quest.isClaimed) textSecondary else textPrimary
                )
                Text(
                    "${quest.currentValue}/${quest.targetValue} — ${quest.description}",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                // Progress bar
                if (!quest.isClaimed) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF21262D))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(quest.progress)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    brush = if (quest.isCompleted) Brush.linearGradient(listOf(successGreen, successGreen))
                                    else Brush.horizontalGradient(listOf(primaryPurple, Color(0xFF00D2FF)))
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Claim button or status
            when {
                quest.isClaimed -> {
                    Text("✅", fontSize = 18.sp)
                }
                quest.isCompleted -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(successGreen)
                            .clickable { onClaim() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Al",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                else -> {
                    // Show reward preview
                    Column(horizontalAlignment = Alignment.End) {
                        if (quest.rewardGold > 0) {
                            Text(
                                "+${quest.rewardGold}⭐",
                                fontSize = 10.sp,
                                color = goldColor
                            )
                        }
                        if (quest.rewardGems > 0) {
                            Text(
                                "+${quest.rewardGems}💎",
                                fontSize = 10.sp,
                                color = Color(0xFFE040FB)
                            )
                        }
                    }
                }
            }
        }
    }
}
