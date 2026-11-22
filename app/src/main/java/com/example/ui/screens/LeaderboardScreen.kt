package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LeaderboardEntry
import com.example.model.TeamRanking
import com.example.model.UiState
import com.example.ui.components.CyberPanel
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.BronzeRank
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SilverRank
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LeaderboardScreen(
    leaderboardState: UiState<List<LeaderboardEntry>>,
    teamRankings: List<TeamRanking>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Player Ranking", "Team Ranking")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "PK Esports Leaderboard",
            subtitle = "Live Match Points + Kill Points = Total Points"
        )
        Spacer(modifier = Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = NeonGreen,
                        activeContentColor = PitchBlack,
                        inactiveContainerColor = CyberCard,
                        inactiveContentColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("leaderboard_tab_$index")
                ) {
                    Text(
                        text = label,
                        fontFamily = ChakraPetchFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (leaderboardState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonGreen)
                }
            }

            is UiState.Error -> {
                EmptyArenaState(title = "Leaderboard Error", subtitle = leaderboardState.message)
            }

            is UiState.Success -> {
                val players = leaderboardState.data
                if (selectedTab == 0) {
                    if (players.isEmpty()) {
                        EmptyArenaState(
                            title = "No Ranked Players Yet",
                            subtitle = "Players appear here immediately after completing sign-up."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(players, key = { _, item -> item.userId }) { index, entry ->
                                val rank = index + 1
                                val isMe = entry.userId == currentUserId
                                RankingRowCard(
                                    rank = rank,
                                    primaryTitle = entry.inGameName,
                                    secondaryTitle = "${entry.teamName} • UID ${entry.freeFireUid}",
                                    wins = entry.totalWins,
                                    matchPoints = entry.matchPoints,
                                    killPoints = entry.killPoints,
                                    totalPoints = entry.totalPoints,
                                    highlighted = isMe
                                )
                            }
                        }
                    }
                } else {
                    if (teamRankings.isEmpty()) {
                        EmptyArenaState(
                            title = "No Teams Ranked Yet",
                            subtitle = "Squads and guilds appear automatically as players join."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(teamRankings, key = { _, item -> item.teamName }) { index, team ->
                                RankingRowCard(
                                    rank = index + 1,
                                    primaryTitle = team.teamName,
                                    secondaryTitle = "${team.memberCount} Roster Player(s)",
                                    wins = team.totalWins,
                                    matchPoints = team.matchPoints,
                                    killPoints = team.killPoints,
                                    totalPoints = team.totalPoints,
                                    highlighted = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingRowCard(
    rank: Int,
    primaryTitle: String,
    secondaryTitle: String,
    wins: Long,
    matchPoints: Long,
    killPoints: Long,
    totalPoints: Long,
    highlighted: Boolean
) {
    val rankColor = when (rank) {
        1 -> GoldTrophy
        2 -> SilverRank
        3 -> BronzeRank
        else -> NeonGreen
    }

    CyberPanel(
        borderColor = if (highlighted) NeonGreen else if (rank <= 3) rankColor.copy(alpha = 0.55f) else CyberBorder,
        containerColor = if (highlighted) CyberCardElevated else CyberCard
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(rankColor.copy(alpha = 0.16f))
                    .border(1.2.dp, rankColor, CircleShape)
            ) {
                if (rank == 1) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Rank 1",
                        tint = GoldTrophy,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "#$rank",
                        fontFamily = BebasNeueFontFamily,
                        fontSize = 18.sp,
                        color = rankColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (highlighted) "$primaryTitle (YOU)" else primaryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (highlighted) NeonGreen else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = secondaryTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$totalPoints PTS",
                    fontFamily = BebasNeueFontFamily,
                    fontSize = 22.sp,
                    color = rankColor
                )
                Text(
                    text = "${wins}W • ${matchPoints}M • ${killPoints}K",
                    fontFamily = ChakraPetchFontFamily,
                    fontSize = 10.sp,
                    color = CyanAccent
                )
            }
        }
    }
}
