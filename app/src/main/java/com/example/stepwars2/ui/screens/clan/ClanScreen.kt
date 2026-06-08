package com.example.stepwars2.ui.screens.clan

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.data.model.Clan
import com.example.stepwars2.data.model.ClanMember
import com.example.stepwars2.data.model.ClanMessage
import com.example.stepwars2.data.repository.ClanRepository
import com.example.stepwars2.data.repository.UserStateManager
import com.example.stepwars2.ui.viewmodel.ClanViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClanScreen(
    viewModel: ClanViewModel = viewModel()
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val goldColor = Color(0xFFFFD700)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)
    val successGreen = Color(0xFF3FB950)
    val errorRed = Color(0xFFFF6B6B)

    val clans by viewModel.clans.collectAsStateWithLifecycle()
    val myClan by viewModel.myClan.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val user by UserStateManager.user.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(primaryPurple.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .padding(top = 16.dp, bottom = 8.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "⚔️ Klanlar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = surfaceDark,
                contentColor = primaryPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = primaryPurple,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text(
                            "\uD83D\uDD0D Klan Bul",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) primaryPurple else textSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text(
                            "⚔️ Klanım",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) primaryPurple else textSecondary
                        )
                    }
                )
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryPurple)
                }
            } else {
                when (selectedTab) {
                    0 -> ClanListTab(
                        clans = clans,
                        userTrophies = user?.trophies ?: 0,
                        userClanId = user?.clanId ?: "",
                        onJoin = { viewModel.joinClan(it) },
                        onCreateClick = { showCreateDialog = true },
                        primaryPurple = primaryPurple,
                        turquoise = turquoise,
                        goldColor = goldColor,
                        surfaceDark = surfaceDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        successGreen = successGreen
                    )
                    1 -> MyClanTab(
                        clan = myClan,
                        members = members,
                        messages = messages,
                        currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        onSendMessage = { viewModel.sendMessage(it) },
                        onKick = { viewModel.kickMember(it) },
                        onPromote = { viewModel.setMemberRole(it, "admin") },
                        onDemote = { viewModel.setMemberRole(it, "member") },
                        onLeave = { viewModel.leaveClan() },
                        primaryPurple = primaryPurple,
                        turquoise = turquoise,
                        goldColor = goldColor,
                        surfaceDark = surfaceDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        errorRed = errorRed
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showCreateDialog) {
            CreateClanDialog(
                userGold = user?.stepGold ?: 0,
                onDismiss = { showCreateDialog = false },
                onCreate = { name, desc, badge, minTrophies ->
                    viewModel.createClan(name, desc, badge, minTrophies)
                    showCreateDialog = false
                },
                primaryPurple = primaryPurple,
                goldColor = goldColor,
                surfaceDark = surfaceDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun ClanListTab(
    clans: List<Clan>,
    userTrophies: Int,
    userClanId: String,
    onJoin: (String) -> Unit,
    onCreateClick: () -> Unit,
    primaryPurple: Color,
    turquoise: Color,
    goldColor: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    successGreen: Color
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (clans.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚔️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Henüz klan yok", fontSize = 16.sp, color = textSecondary)
                Text("İlk klanı sen oluştur!", fontSize = 13.sp, color = textSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = 12.dp, bottom = 80.dp
                )
            ) {
                items(clans, key = { it.id }) { clan ->
                    ClanCard(
                        clan = clan,
                        rank = clans.indexOf(clan) + 1,
                        canJoin = userClanId.isEmpty() && userTrophies >= clan.minTrophies,
                        onJoin = { onJoin(clan.id) },
                        primaryPurple = primaryPurple,
                        turquoise = turquoise,
                        goldColor = goldColor,
                        surfaceDark = surfaceDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        successGreen = successGreen
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = primaryPurple,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Klan Oluştur")
        }
    }
}

@Composable
private fun ClanCard(
    clan: Clan,
    rank: Int,
    canJoin: Boolean,
    onJoin: () -> Unit,
    primaryPurple: Color,
    turquoise: Color,
    goldColor: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    successGreen: Color
) {
    val rankEmoji = when (rank) {
        1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rankEmoji,
                fontSize = if (rank <= 3) 22.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primaryPurple.copy(alpha = 0.3f), turquoise.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(clan.badge, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clan.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("👥 ${clan.memberCount}/${ClanRepository.MAX_MEMBERS}", fontSize = 11.sp, color = textSecondary)
                    Text("⚡ ${clan.totalPower}", fontSize = 11.sp, color = turquoise)
                    if (clan.minTrophies > 0) {
                        Text("🏆 ${clan.minTrophies}+", fontSize = 11.sp, color = goldColor)
                    }
                }
            }
            Button(
                onClick = onJoin,
                enabled = canJoin && clan.memberCount < ClanRepository.MAX_MEMBERS,
                colors = ButtonDefaults.buttonColors(
                    containerColor = successGreen,
                    disabledContainerColor = Color(0xFF21262D)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Katıl", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MyClanTab(
    clan: Clan?,
    members: List<ClanMember>,
    messages: List<ClanMessage>,
    currentUid: String,
    onSendMessage: (String) -> Unit,
    onKick: (String) -> Unit,
    onPromote: (String) -> Unit,
    onDemote: (String) -> Unit,
    onLeave: () -> Unit,
    primaryPurple: Color,
    turquoise: Color,
    goldColor: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color,
    errorRed: Color
) {
    if (clan == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("\uD83C\uDFF0", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Henüz bir klana üye değilsin", fontSize = 16.sp, color = textPrimary)
            Text("'Klan Bul' sekmesinden bir klana katıl!", fontSize = 13.sp, color = textSecondary)
        }
        return
    }

    val currentMember = members.find { it.uid == currentUid }
    val isLeader = currentMember?.role == "leader"
    val isAdmin = currentMember?.role == "admin"

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 8.dp)
        ) {
            // Clan Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(clan.badge, fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(clan.name, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = textPrimary)
                        if (clan.description.isNotEmpty()) {
                            Text(clan.description, fontSize = 13.sp, color = textSecondary, textAlign = TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡", fontSize = 18.sp)
                                Text("${clan.totalPower}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = turquoise)
                                Text("Güç", fontSize = 10.sp, color = textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👥", fontSize = 18.sp)
                                Text("${clan.memberCount}/${ClanRepository.MAX_MEMBERS}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Üye", fontSize = 10.sp, color = textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏆", fontSize = 18.sp)
                                Text("${clan.minTrophies}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = goldColor)
                                Text("Min Kupa", fontSize = 10.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("👥 Üyeler (${members.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }

            items(members, key = { it.uid }) { member ->
                MemberRow(
                    member = member,
                    isCurrentUser = member.uid == currentUid,
                    isLeader = isLeader,
                    isAdmin = isAdmin,
                    onKick = { onKick(member.uid) },
                    onPromote = { onPromote(member.uid) },
                    onDemote = { onDemote(member.uid) },
                    primaryPurple = primaryPurple,
                    turquoise = turquoise,
                    goldColor = goldColor,
                    surfaceDark = surfaceDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }

            item {
                if (!isLeader) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onLeave,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = errorRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Klandan Ayrıl", fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("\uD83D\uDCAC Klan Sohbeti", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            }

            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Henüz mesaj yok. İlk mesajı gönder! \uD83D\uDCAC", fontSize = 13.sp, color = textSecondary)
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        isOwn = msg.senderUid == currentUid,
                        primaryPurple = primaryPurple,
                        surfaceDark = surfaceDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(4.dp)) }
        }

        ChatInput(
            onSend = onSendMessage,
            primaryPurple = primaryPurple,
            surfaceDark = surfaceDark,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
    }
}

@Composable
private fun MemberRow(
    member: ClanMember,
    isCurrentUser: Boolean,
    isLeader: Boolean,
    isAdmin: Boolean,
    onKick: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    primaryPurple: Color,
    turquoise: Color,
    goldColor: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val roleColor = when (member.role) {
        "leader" -> goldColor; "admin" -> turquoise; else -> textSecondary
    }
    val roleEmoji = when (member.role) {
        "leader" -> "👑"; "admin" -> "🛡️"; else -> "⚔️"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isCurrentUser) Modifier.border(1.dp, primaryPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) primaryPurple.copy(alpha = 0.08f) else surfaceDark
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(roleColor.copy(alpha = 0.4f), roleColor.copy(alpha = 0.2f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(member.username.firstOrNull()?.uppercase() ?: "?", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        member.username + if (isCurrentUser) " (Sen)" else "",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = if (isCurrentUser) primaryPurple else textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("$roleEmoji ${member.getRoleDisplayName()}", fontSize = 10.sp, color = roleColor, fontWeight = FontWeight.Bold)
                }
                Text("Lv.${member.level} • 🏆 ${member.trophies} • ⚡ ${member.power}", fontSize = 11.sp, color = textSecondary)
            }

            if (!isCurrentUser && member.role != "leader") {
                if (isLeader) {
                    if (member.role == "member") {
                        IconButton(onClick = onPromote, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Shield, "Yönetici Yap", tint = turquoise, modifier = Modifier.size(18.dp))
                        }
                    } else if (member.role == "admin") {
                        IconButton(onClick = onDemote, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Shield, "Üye Yap", tint = textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onKick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.PersonRemove, "At", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                    }
                } else if (isAdmin && member.role == "member") {
                    IconButton(onClick = onKick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.PersonRemove, "At", tint = Color(0xFFFF6B6B), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ClanMessage,
    isOwn: Boolean,
    primaryPurple: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        if (!isOwn) {
            Text(message.senderName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = primaryPurple, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
        }
        Box(
            modifier = Modifier
                .background(
                    if (isOwn) primaryPurple.copy(alpha = 0.2f) else surfaceDark,
                    RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = if (isOwn) 14.dp else 4.dp, bottomEnd = if (isOwn) 4.dp else 14.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(message.text, fontSize = 13.sp, color = textPrimary)
                Text(timeFormat.format(Date(message.timestamp)), fontSize = 9.sp, color = textSecondary, modifier = Modifier.align(Alignment.End))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun ChatInput(
    onSend: (String) -> Unit,
    primaryPurple: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    var text by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier.fillMaxWidth().background(surfaceDark).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Mesaj yaz...", color = textSecondary, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary,
                focusedBorderColor = primaryPurple, unfocusedBorderColor = Color(0xFF30363D),
                cursorColor = primaryPurple, focusedContainerColor = Color(0xFF0D1117), unfocusedContainerColor = Color(0xFF0D1117)
            ),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (text.isNotBlank()) { onSend(text); text = ""; focusManager.clearFocus() }
            })
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = {
            if (text.isNotBlank()) { onSend(text); text = ""; focusManager.clearFocus() }
        }) {
            Icon(Icons.Filled.Send, "Gönder", tint = if (text.isNotBlank()) primaryPurple else textSecondary)
        }
    }
}

@Composable
private fun CreateClanDialog(
    userGold: Int,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int) -> Unit,
    primaryPurple: Color,
    goldColor: Color,
    surfaceDark: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedBadge by remember { mutableStateOf("⚔️") }
    var minTrophies by remember { mutableFloatStateOf(0f) }

    val badges = listOf("⚔️", "🛡️", "🏰", "🐉", "🦁", "🐺", "🔥", "💀", "⭐", "💎", "🗡️", "🏹", "🎯", "👑", "🦅", "🐍", "☠️", "🌙", "⚡", "🌟", "🔱", "🎖️", "🏆", "💪")
    val canCreate = name.isNotBlank() && userGold >= ClanRepository.CREATE_COST

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceDark,
        title = { Text("🏰 Klan Oluştur", fontWeight = FontWeight.Bold, color = textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { if (it.length <= 20) name = it },
                    label = { Text("Klan Adı", color = textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryPurple, unfocusedBorderColor = Color(0xFF30363D), cursorColor = primaryPurple),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description, onValueChange = { if (it.length <= 100) description = it },
                    label = { Text("Açıklama", color = textSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, focusedBorderColor = primaryPurple, unfocusedBorderColor = Color(0xFF30363D), cursorColor = primaryPurple),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("Klan Amblemi", fontSize = 13.sp, color = textSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                val rows = badges.chunked(8)
                rows.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { badge ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (badge == selectedBadge) primaryPurple.copy(alpha = 0.3f) else Color.Transparent)
                                    .then(if (badge == selectedBadge) Modifier.border(1.5.dp, primaryPurple, RoundedCornerShape(8.dp)) else Modifier)
                                    .clickable { selectedBadge = badge },
                                contentAlignment = Alignment.Center
                            ) { Text(badge, fontSize = 18.sp) }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Minimum Kupa: ${minTrophies.toInt()}", fontSize = 13.sp, color = textSecondary)
                Slider(
                    value = minTrophies, onValueChange = { minTrophies = it },
                    valueRange = 0f..500f, steps = 9,
                    colors = SliderDefaults.colors(thumbColor = primaryPurple, activeTrackColor = primaryPurple)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF21262D), RoundedCornerShape(10.dp)).padding(10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Maliyet: ", fontSize = 14.sp, color = textSecondary)
                    Icon(Icons.Filled.Star, null, tint = goldColor, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${ClanRepository.CREATE_COST}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (canCreate) goldColor else Color(0xFFFF6B6B))
                    Text(" (Bakiye: $userGold)", fontSize = 11.sp, color = textSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name.trim(), description.trim(), selectedBadge, minTrophies.toInt()) },
                enabled = canCreate,
                colors = ButtonDefaults.buttonColors(containerColor = primaryPurple, disabledContainerColor = Color(0xFF21262D))
            ) { Text("Oluştur ⚔️", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal", color = textSecondary) }
        }
    )
}
