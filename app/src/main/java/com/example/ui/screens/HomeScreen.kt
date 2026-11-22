package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Announcement
import com.example.model.LeaderboardEntry
import com.example.model.PlayerProfile
import com.example.model.Tournament
import com.example.model.TournamentMode
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.ui.components.CyberPanel
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.NeonStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatKpiCard
import com.example.ui.components.TournamentCard
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    profile: PlayerProfile,
    tournamentsState: UiState<List<Tournament>>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    announcementsState: UiState<List<Announcement>>,
    joinedTournamentIds: Set<String>,
    onOpenModeFilter: (TournamentMode) -> Unit,
    onOpenTournaments: () -> Unit,
    onSelectTournament: (String) -> Unit,
    onOpenWallet: (WalletQuickAction?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val waNumber = stringResource(R.string.whatsapp_support_number)
    val tournaments = (tournamentsState as? UiState.Success)?.data.orEmpty()
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()

    val totalPlayers = leaderboard.size
    val totalTournaments = tournaments.size
    val totalWinners = leaderboard.sumOf { it.totalWins }.coerceAtLeast(
        tournaments.count { it.winnerName.isNotBlank() }.toLong()
    )
    val featuredTournaments = tournaments.filter {
        it.status == TournamentStatus.LIVE.raw || it.status == TournamentStatus.UPCOMING.raw
    }.take(5)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Gaming Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.5.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_gamerzone_banner),
                    contentDescription = stringResource(R.string.cd_banner_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PitchBlack.copy(alpha = 0.92f), PitchBlack.copy(alpha = 0.4f))
                            )
                        )
                )
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonStatusBadge(text = "GAMERZONE PK", color = NeonGreen)
                        NeonStatusBadge(text = "IGN: ${profile.inGameName}", color = CyanAccent)
                    }

                    Column {
                        Text(
                            text = "FREE FIRE ESPORTS",
                            fontFamily = RussoOneFontFamily,
                            fontSize = 24.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Daily Solo, Duo, Squad & Clash Squad PKR Rooms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onOpenTournaments,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = PitchBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("home_explore_tournaments_button")
                        ) {
                            Text(
                                text = "BROWSE TOURNAMENTS",
                                fontFamily = ChakraPetchFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 1b. My Wallet Quick Bar
        item {
            CyberPanel(
                borderColor = NeonGreen.copy(alpha = 0.5f),
                onClick = { onOpenWallet(null) },
                modifier = Modifier.testTag("home_wallet_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MY WALLET BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${profile.walletBalancePkr} PKR",
                                fontFamily = BebasNeueFontFamily,
                                fontSize = 26.sp,
                                color = NeonGreen
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onOpenWallet(WalletQuickAction.ADD_MONEY) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("+ Add", fontFamily = ChakraPetchFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { onOpenWallet(WalletQuickAction.WITHDRAW) },
                            border = BorderStroke(1.dp, CyanAccent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("Withdraw", color = CyanAccent, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 2. Platform Live KPIs: Total Players, Total Tournaments, Total Winners
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard(
                    label = "Total Players",
                    value = totalPlayers.toString(),
                    icon = Icons.Filled.Groups,
                    accent = NeonGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_total_players")
                )
                StatKpiCard(
                    label = "Tournaments",
                    value = totalTournaments.toString(),
                    icon = Icons.Filled.SportsEsports,
                    accent = CyanAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_total_tournaments")
                )
                StatKpiCard(
                    label = "Total Winners",
                    value = totalWinners.toString(),
                    icon = Icons.Filled.EmojiEvents,
                    accent = GoldTrophy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("kpi_total_winners")
                )
            }
        }

        // 3. Tournament Modes Quick Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Game Modes", subtitle = "Jump directly into your squad format")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TournamentMode.entries.forEach { mode ->
                        FilterChip(
                            selected = false,
                            onClick = { onOpenModeFilter(mode) },
                            label = {
                                Text(
                                    text = mode.label,
                                    fontFamily = ChakraPetchFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CyberCardElevated,
                                labelColor = NeonGreen
                            ),
                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("home_mode_${mode.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // 4. Upcoming & Live Tournaments
        item {
            SectionHeader(
                title = "Upcoming & Live",
                subtitle = "Real-time matches & prize pools",
                actionLabel = "View All",
                onActionClick = onOpenTournaments
            )
        }

        when (tournamentsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                }
            }

            is UiState.Error -> {
                item {
                    EmptyArenaState(
                        title = "Could not load tournaments",
                        subtitle = tournamentsState.message
                    )
                }
            }

            is UiState.Success -> {
                if (featuredTournaments.isEmpty()) {
                    item {
                        EmptyArenaState(
                            title = "No Upcoming Matches Yet",
                            subtitle = "Admins can publish upcoming Free Fire tournaments from the Admin Dashboard."
                        )
                    }
                } else {
                    items(featuredTournaments, key = { it.id }) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            isJoined = joinedTournamentIds.contains(tournament.id),
                            onClick = { onSelectTournament(tournament.id) }
                        )
                    }
                }
            }
        }

        // 5. Latest Announcements
        if (announcements.isNotEmpty()) {
            item {
                SectionHeader(title = "Arena Announcements")
            }
            items(announcements.take(3), key = { it.id }) { ann ->
                CyberPanel(borderColor = CyanAccent.copy(alpha = 0.4f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = ann.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = ann.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 6. Quick WhatsApp Support Banner
        item {
            CyberPanel(
                borderColor = NeonGreen.copy(alpha = 0.5f),
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/$waNumber?text=Hi%20Gamer%20Zone%20PK%20Support!")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.testTag("home_whatsapp_support")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEED HELP OR ENTRY VERIFICATION?",
                            style = MaterialTheme.typography.titleMedium,
                            color = NeonGreen
                        )
                        Text(
                            text = "Tap to chat with GamerZone PK WhatsApp Support",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.Chat,
                        contentDescription = "WhatsApp Support",
                        tint = NeonGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
