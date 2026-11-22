package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Announcement
import com.example.model.AnnouncementCategory
import com.example.model.PlayerProfile
import com.example.model.TournamentRegistration
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.ui.components.CyberPanel
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.NeonStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatKpiCard
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PlayerDashboardScreen(
    profile: PlayerProfile,
    isAdmin: Boolean,
    myRegistrationsState: UiState<List<TournamentRegistration>>,
    announcementsState: UiState<List<Announcement>>,
    isBusy: Boolean,
    onOpenTournament: (String) -> Unit,
    onOpenWallet: (WalletQuickAction?) -> Unit,
    onUpdateProfile: (displayName: String, ign: String, ffUid: String, team: String, whatsapp: String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    val sections = listOf("My Matches", "History", "My Profile", "Alerts", "Support")
    val registrations = (myRegistrationsState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "Player Dashboard",
            subtitle = "${profile.inGameName} • ${if (isAdmin) "ADMIN ACCESS" else "VERIFIED PLAYER"}"
        )

        Spacer(modifier = Modifier.height(10.dp))

        CyberPanel(
            borderColor = NeonGreen.copy(alpha = 0.45f),
            onClick = { onOpenWallet(null) },
            modifier = Modifier.testTag("player_wallet_card")
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("WALLET BALANCE", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "${profile.walletBalancePkr} PKR",
                            fontFamily = BebasNeueFontFamily,
                            fontSize = 22.sp,
                            color = NeonGreen
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onOpenWallet(WalletQuickAction.TOURNAMENT_PAYMENTS) },
                        border = BorderStroke(1.dp, CyanAccent),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Fee History", color = CyanAccent, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onOpenWallet(WalletQuickAction.ADD_MONEY) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("+ Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sections.forEachIndexed { idx, title ->
                FilterChip(
                    selected = selectedSection == idx,
                    onClick = { selectedSection = idx },
                    label = {
                        Text(
                            text = title,
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = PitchBlack,
                        containerColor = CyberCard,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selectedSection == idx) NeonGreen else CyberBorder),
                    modifier = Modifier.testTag("player_tab_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedSection) {
            0 -> {
                val active = registrations.filter { it.tournamentStatus != TournamentStatus.COMPLETED.raw }
                RegistrationListSection(
                    items = active,
                    emptyTitle = "No Active Tournaments",
                    emptySubtitle = "Join any Solo, Duo, Squad, or Clash Squad match to see your slot here.",
                    onOpenTournament = onOpenTournament
                )
            }

            1 -> {
                val completed = registrations.filter { it.tournamentStatus == TournamentStatus.COMPLETED.raw }
                RegistrationListSection(
                    items = completed,
                    emptyTitle = "No Completed Match History",
                    emptySubtitle = "Your past tournament placements and kill points will appear here.",
                    onOpenTournament = onOpenTournament
                )
            }

            2 -> {
                ProfileManagementSection(
                    profile = profile,
                    isAdmin = isAdmin,
                    isBusy = isBusy,
                    onUpdateProfile = onUpdateProfile,
                    onSignOut = onSignOut
                )
            }

            3 -> {
                NotificationsSection(announcements = announcements)
            }

            4 -> {
                SupportAndInfoSection()
            }
        }
    }
}

@Composable
private fun RegistrationListSection(
    items: List<TournamentRegistration>,
    emptyTitle: String,
    emptySubtitle: String,
    onOpenTournament: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyArenaState(title = emptyTitle, subtitle = emptySubtitle)
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.tournamentId }) { reg ->
            CyberPanel(
                borderColor = NeonGreen.copy(alpha = 0.45f),
                onClick = { onOpenTournament(reg.tournamentId) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonStatusBadge(text = reg.tournamentMode, color = CyanAccent)
                    NeonStatusBadge(text = "SLOT #${reg.slotNumber}", color = NeonGreen)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reg.tournamentTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "${reg.mapName} • ${reg.matchTime}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (reg.totalPoints > 0 || reg.placement > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Placement #${reg.placement} • Match ${reg.matchPoints} + Kills ${reg.killPoints} = ${reg.totalPoints} PTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldTrophy
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileManagementSection(
    profile: PlayerProfile,
    isAdmin: Boolean,
    isBusy: Boolean,
    onUpdateProfile: (displayName: String, ign: String, ffUid: String, team: String, whatsapp: String) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var ign by remember(profile.inGameName) { mutableStateOf(profile.inGameName) }
    var ffUid by remember(profile.freeFireUid) { mutableStateOf(profile.freeFireUid) }
    var teamName by remember(profile.teamName) { mutableStateOf(profile.teamName) }
    var whatsapp by remember(profile.whatsappNumber) { mutableStateOf(profile.whatsappNumber) }
    var profileError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.imePadding()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard("Matches", "${profile.matchesPlayed}", Icons.Filled.Groups, CyanAccent, Modifier.weight(1f))
                StatKpiCard("Wins", "${profile.totalWins}", Icons.Filled.Campaign, GoldTrophy, Modifier.weight(1f))
                StatKpiCard("Points", "${profile.totalPoints}", Icons.Filled.Person, NeonGreen, Modifier.weight(1f))
            }
        }

        item {
            CyberPanel(borderColor = NeonGreen.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCOUNT DETAILS",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGreen
                    )
                    NeonStatusBadge(
                        text = if (isAdmin) "ADMIN" else "PLAYER",
                        color = if (isAdmin) GoldTrophy else NeonGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileInputField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Display Name",
                        icon = Icons.Filled.Person,
                        testTag = "profile_display_input"
                    )
                    ProfileInputField(
                        value = ign,
                        onValueChange = { ign = it },
                        label = "Free Fire In-Game Name",
                        icon = Icons.Filled.Person,
                        testTag = "profile_ign_input"
                    )
                    ProfileInputField(
                        value = ffUid,
                        onValueChange = { ffUid = it.filter { c -> c.isDigit() } },
                        label = "Free Fire UID",
                        icon = Icons.Filled.Fingerprint,
                        keyboardType = KeyboardType.Number,
                        testTag = "profile_ffuid_input"
                    )
                    ProfileInputField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = "Squad / Clan Name",
                        icon = Icons.Filled.Groups,
                        testTag = "profile_team_input"
                    )
                    ProfileInputField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = "WhatsApp Number",
                        icon = Icons.Filled.Phone,
                        keyboardType = KeyboardType.Phone,
                        testTag = "profile_whatsapp_input"
                    )
                }

                if (profileError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = profileError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonLive
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val cleanDisplay = displayName.trim()
                        val cleanIgn = ign.trim()
                        val cleanUid = ffUid.trim()
                        val cleanTeam = teamName.trim()
                        val cleanWa = whatsapp.trim()
                        when {
                            cleanDisplay.isEmpty() || cleanDisplay.length > 60 ->
                                profileError = "Display name must be 1-60 characters."
                            cleanIgn.length !in 2..40 ->
                                profileError = "In-Game Name must be 2-40 characters."
                            cleanUid.length !in 4..20 ->
                                profileError = "Free Fire UID must be 4-20 digits."
                            cleanTeam.length !in 1..40 ->
                                profileError = "Squad / Clan name must be 1-40 characters."
                            cleanWa.length > 25 ->
                                profileError = "WhatsApp number must be at most 25 characters."
                            else -> {
                                profileError = null
                                onUpdateProfile(cleanDisplay, cleanIgn, cleanUid, cleanTeam, cleanWa)
                            }
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE PROFILE",
                        fontFamily = RussoOneFontFamily,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, CrimsonLive),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLive),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "SIGN OUT")
            }
        }
    }
}

@Composable
private fun NotificationsSection(announcements: List<Announcement>) {
    if (announcements.isEmpty()) {
        EmptyArenaState(
            title = "No Notifications",
            subtitle = "Tournament announcements, room updates, and winners will appear here."
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(announcements, key = { it.id }) { ann ->
            val cat = AnnouncementCategory.fromRaw(ann.category)
            val color = when (cat) {
                AnnouncementCategory.WINNER -> GoldTrophy
                AnnouncementCategory.ALERT -> CrimsonLive
                AnnouncementCategory.RULES -> CyanAccent
                AnnouncementCategory.TOURNAMENT -> NeonGreen
            }
            CyberPanel(borderColor = color.copy(alpha = 0.5f)) {
                NeonStatusBadge(text = cat.label, color = color)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = ann.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ann.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SupportAndInfoSection() {
    val context = LocalContext.current
    val waNumber = stringResource(R.string.whatsapp_support_number)
    val supportEmail = stringResource(R.string.support_email)

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // WhatsApp Support
        item {
            CyberPanel(borderColor = NeonGreen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Chat, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WHATSAPP SUPPORT", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Instant help for PKR entry verification, custom room IDs, or prize payouts (10:00 AM – 11:00 PM PKT).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/$waNumber?text=Assalam%20o%20Alaikum%20Gamer%20Zone%20PK%20Support!")
                        )
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                Toast.makeText(context, "No browser or WhatsApp app found.", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("support_whatsapp_button")
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CHAT ON WHATSAPP", fontFamily = RussoOneFontFamily)
                }
            }
        }

        // Contact Page
        item {
            CyberPanel(borderColor = CyanAccent.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Email, contentDescription = null, tint = CyanAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CONTACT GAMERZONE PK", style = MaterialTheme.typography.titleMedium, color = CyanAccent)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Official Email: $supportEmail", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                Text("Support Hours: Daily 10:00 AM – 11:00 PM PKT", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Region: Pakistan (PKR Esports Tournaments)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$supportEmail"))
                        runCatching { context.startActivity(emailIntent) }
                            .onFailure {
                                Toast.makeText(context, "No email app installed on this device.", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("support_email_button")
                ) {
                    Icon(Icons.Filled.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEND EMAIL")
                }
            }
        }

        // About Us
        item {
            CyberPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ABOUT US", style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "GAMERZONE PK is a competitive Pakistani Free Fire esports platform built for mobile warriors across Karachi, Lahore, Islamabad, and nationwide. We host daily fair-play Solo, Duo, Squad, Clash Squad, and Battle Royale tournaments with transparent PKR prize pools and live player rankings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }

        // Rules & Regulations
        item {
            CyberPanel(borderColor = GoldTrophy.copy(alpha = 0.45f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = GoldTrophy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RULES & REGULATIONS", style = MaterialTheme.typography.titleMedium, color = GoldTrophy)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Mobile players only — PC emulators, scripts, or third-party injectors result in a permanent ban.\n" +
                        "2. Your registered Free Fire UID and IGN must match the account entering the custom room.\n" +
                        "3. Join custom rooms at least 10 minutes before match time using the Room ID & Password.\n" +
                        "4. Teaming in Solo/Duo or toxic behavior forfeits all match points and prizes.\n" +
                        "5. Leaderboard scoring combines Match Placement Points and Kill Points published by Admins.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
