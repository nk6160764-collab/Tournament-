package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerProfile
import com.example.model.Tournament
import com.example.model.TournamentMode
import com.example.model.TournamentRegistration
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.ui.components.CyberPanel
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.NeonStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.TournamentCard
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.SilverRank
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TournamentsScreen(
    tournamentsState: UiState<List<Tournament>>,
    joinedTournamentIds: Set<String>,
    initialModeFilter: TournamentMode?,
    onClearInitialMode: () -> Unit,
    onSelectTournament: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by remember { mutableStateOf<TournamentStatus?>(null) }
    var selectedMode by remember(initialModeFilter) { mutableStateOf(initialModeFilter) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "Free Fire Tournaments",
            subtitle = "Solo, Duo, Squad, Clash Squad & Battle Royale"
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Status filters (All, Upcoming, Live, Completed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusFilterChip(
                label = "All",
                selected = selectedStatus == null,
                onClick = { selectedStatus = null },
                tag = "filter_status_all"
            )
            TournamentStatus.entries.forEach { st ->
                StatusFilterChip(
                    label = st.label,
                    selected = selectedStatus == st,
                    onClick = { selectedStatus = st },
                    tag = "filter_status_${st.raw}"
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Mode filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusFilterChip(
                label = "All Modes",
                selected = selectedMode == null,
                onClick = {
                    selectedMode = null
                    onClearInitialMode()
                },
                tag = "filter_mode_all"
            )
            TournamentMode.entries.forEach { mode ->
                StatusFilterChip(
                    label = mode.label,
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                    tag = "filter_mode_${mode.name.lowercase()}"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (tournamentsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }

            is UiState.Error -> {
                EmptyArenaState(title = "Tournament Error", subtitle = tournamentsState.message)
            }

            is UiState.Success -> {
                val filtered = tournamentsState.data.filter { t ->
                    val matchStatus = selectedStatus == null || t.status.equals(selectedStatus?.raw, ignoreCase = true)
                    val matchMode = selectedMode == null || t.mode.equals(selectedMode?.label, ignoreCase = true)
                    matchStatus && matchMode
                }

                if (filtered.isEmpty()) {
                    EmptyArenaState(
                        title = "No Matches Found",
                        subtitle = "Try switching status or mode filters, or check back soon."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { tournament ->
                            TournamentCard(
                                tournament = tournament,
                                isJoined = joinedTournamentIds.contains(tournament.id),
                                onClick = { onSelectTournament(tournament.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonGreen,
            selectedLabelColor = PitchBlack,
            containerColor = CyberCard,
            labelColor = TextSecondary
        ),
        border = BorderStroke(1.dp, if (selected) NeonGreen else CyberBorder),
        modifier = Modifier.testTag(tag)
    )
}

@Composable
fun TournamentDetailScreen(
    tournament: Tournament,
    profile: PlayerProfile,
    isAdmin: Boolean,
    isInitiallyJoined: Boolean = false,
    registrationsState: UiState<List<TournamentRegistration>>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onJoinTournament: (Tournament, PlayerProfile) -> Unit,
    onOpenAddMoney: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showJoinConfirm by remember { mutableStateOf(false) }

    val registrations = (registrationsState as? UiState.Success)?.data.orEmpty()
    val isJoined = isInitiallyJoined || registrations.any { it.userId == profile.uid }
    val status = TournamentStatus.fromRaw(tournament.status)
    val isFull = tournament.registeredCount >= tournament.totalSlots
    val canJoin = !isJoined && !isFull && status == TournamentStatus.UPCOMING && registrationsState !is UiState.Loading
    val hasEnoughBalance = profile.walletBalancePkr >= tournament.entryFeePkr
    val progress = if (tournament.totalSlots > 0) {
        (tournament.registeredCount.toFloat() / tournament.totalSlots.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        // Top Detail Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to tournaments",
                    tint = NeonGreen
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TOURNAMENT DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen
                )
                Text(
                    text = tournament.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Core Info Panel
            item {
                CyberPanel(borderColor = NeonGreen.copy(alpha = 0.5f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val stColor = when (status) {
                            TournamentStatus.LIVE -> CrimsonLive
                            TournamentStatus.UPCOMING -> NeonGreen
                            TournamentStatus.COMPLETED -> GoldTrophy
                        }
                        NeonStatusBadge(text = status.label, color = stColor)
                        NeonStatusBadge(text = tournament.mode, color = CyanAccent)
                        NeonStatusBadge(text = tournament.mapName, color = GoldTrophy)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tournament.title,
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailInfoBox(
                            label = "ENTRY FEE",
                            value = if (tournament.entryFeePkr == 0L) "FREE" else "${tournament.entryFeePkr} PKR",
                            accent = NeonGreen,
                            modifier = Modifier.weight(1f)
                        )
                        DetailInfoBox(
                            label = "PRIZE POOL",
                            value = "${tournament.prizePoolPkr} PKR",
                            accent = GoldTrophy,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailInfoBox(
                            label = "MATCH TIME",
                            value = tournament.matchTime,
                            accent = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        DetailInfoBox(
                            label = "MAP",
                            value = tournament.mapName,
                            accent = NeonGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SLOTS FILLED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${tournament.registeredCount} / ${tournament.totalSlots}",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = if (isFull) CrimsonLive else NeonGreen,
                        trackColor = CyberCardElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            // Prize Breakdown
            item {
                CyberPanel(borderColor = GoldTrophy.copy(alpha = 0.45f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = GoldTrophy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRIZE DISTRIBUTION",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldTrophy
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailInfoBox("1ST PRIZE", "${tournament.firstPrizePkr} PKR", GoldTrophy, Modifier.weight(1f))
                        DetailInfoBox("2ND PRIZE", "${tournament.secondPrizePkr} PKR", SilverRank, Modifier.weight(1f))
                        DetailInfoBox("3RD PRIZE", "${tournament.thirdPrizePkr} PKR", BronzeRank, Modifier.weight(1f))
                    }
                }
            }

            // Room ID & Password (Visible to registered players & admins)
            if ((isJoined || isAdmin) && tournament.roomId.isNotBlank()) {
                item {
                    CyberPanel(borderColor = CyanAccent) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CUSTOM ROOM CREDENTIALS",
                                style = MaterialTheme.typography.titleMedium,
                                color = CyanAccent
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailInfoBox("ROOM ID", tournament.roomId, CyanAccent, Modifier.weight(1f))
                            DetailInfoBox("PASSWORD", tournament.roomPassword.ifBlank { "Open" }, NeonGreen, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Winner Card if completed
            if (tournament.winnerName.isNotBlank()) {
                item {
                    CyberPanel(borderColor = GoldTrophy) {
                        Text(
                            text = "CHAMPION / WINNER",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldTrophy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "BOOYAH! ${tournament.winnerName}",
                            fontFamily = RussoOneFontFamily,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Tournament Rules
            item {
                CyberPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TOURNAMENT RULES",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = tournament.rules,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }

            // Registered Players Roster
            item {
                SectionHeader(
                    title = "Registered Players (${registrations.size})",
                    subtitle = "Verified Free Fire UIDs & Squads"
                )
            }

            when (registrationsState) {
                is UiState.Loading -> {
                    item { CircularProgressIndicator(color = NeonGreen) }
                }

                is UiState.Error -> {
                    item {
                        EmptyArenaState("Unable to load roster", registrationsState.message)
                    }
                }

                is UiState.Success -> {
                    if (registrations.isEmpty()) {
                        item {
                            EmptyArenaState(
                                title = "No Players Joined Yet",
                                subtitle = "Be the first squad to claim Slot #1!"
                            )
                        }
                    } else {
                        items(registrations, key = { it.userId }) { reg ->
                            CyberPanel(
                                borderColor = if (reg.userId == profile.uid) NeonGreen else CyberBorder
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Slot #${reg.slotNumber} • ${reg.inGameName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (reg.userId == profile.uid) NeonGreen else TextPrimary
                                        )
                                        Text(
                                            text = "${reg.teamName} • FF UID: ${reg.freeFireUid}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    if (reg.totalPoints > 0 || reg.placement > 0) {
                                        NeonStatusBadge(
                                            text = "#${reg.placement} • ${reg.totalPoints} PTS",
                                            color = GoldTrophy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Join Footer CTA
        Surface(
            color = CyberCard,
            border = BorderStroke(1.dp, CyberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canJoin && tournament.entryFeePkr > 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wallet Balance: ${profile.walletBalancePkr} PKR",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (hasEnoughBalance) NeonGreen else CrimsonLive
                        )
                        if (!hasEnoughBalance) {
                            TextButton(
                                onClick = onOpenAddMoney,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Add Money +", color = NeonGreen)
                            }
                        }
                    }
                }
                Button(
                    onClick = { showJoinConfirm = true },
                    enabled = canJoin && !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isJoined) CyanAccent else NeonGreen,
                        contentColor = PitchBlack,
                        disabledContainerColor = CyberCardElevated,
                        disabledContentColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("join_tournament_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = PitchBlack,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        val buttonText = when {
                            isJoined -> "ALREADY REGISTERED"
                            status != TournamentStatus.UPCOMING -> "REGISTRATION CLOSED (${status.label.uppercase()})"
                            isFull -> "SLOTS FULL"
                            else -> "JOIN TOURNAMENT • ${if (tournament.entryFeePkr == 0L) "FREE" else "${tournament.entryFeePkr} PKR"}"
                        }
                        if (isJoined) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = buttonText,
                            fontFamily = RussoOneFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showJoinConfirm) {
        val afterBalance = profile.walletBalancePkr - tournament.entryFeePkr
        AlertDialog(
            onDismissRequest = { showJoinConfirm = false },
            containerColor = CyberSurface,
            title = {
                Text(
                    text = if (hasEnoughBalance) "Confirm Entry & Payment" else "Insufficient Wallet Balance",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (hasEnoughBalance) NeonGreen else CrimsonLive
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${tournament.title} (${tournament.mode} • ${tournament.mapName})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Entry Fee: ${if (tournament.entryFeePkr == 0L) "FREE" else "${tournament.entryFeePkr} PKR"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GoldTrophy
                    )
                    Text(
                        text = "Your Wallet Balance: ${profile.walletBalancePkr} PKR",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    if (hasEnoughBalance) {
                        if (tournament.entryFeePkr > 0L) {
                            Text(
                                text = "Balance After Confirmation: $afterBalance PKR",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonGreen
                            )
                        }
                        Text(
                            text = "Slot will be booked under IGN: ${profile.inGameName} (UID: ${profile.freeFireUid}).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "You need ${tournament.entryFeePkr - profile.walletBalancePkr} more PKR in your wallet to join this tournament. Deposit via EasyPaisa or JazzCash.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonLive
                        )
                    }
                }
            },
            confirmButton = {
                if (hasEnoughBalance) {
                    Button(
                        onClick = {
                            showJoinConfirm = false
                            onJoinTournament(tournament, profile)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                        modifier = Modifier.testTag("confirm_join_pay_button")
                    ) {
                        Text(
                            text = if (tournament.entryFeePkr == 0L) "Confirm Join" else "Confirm & Pay ${tournament.entryFeePkr} PKR",
                            fontFamily = RussoOneFontFamily,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            showJoinConfirm = false
                            onOpenAddMoney()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                        modifier = Modifier.testTag("insufficient_add_money_button")
                    ) {
                        Text("Add Money to Wallet", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailInfoBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CyberCardElevated,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = accent
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = BebasNeueFontFamily,
                fontSize = 20.sp,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}
