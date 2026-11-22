package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdminWalletTransaction
import com.example.model.Announcement
import com.example.model.AnnouncementCategory
import com.example.model.DepositRequest
import com.example.model.LeaderboardEntry
import com.example.model.PaymentMethod
import com.example.model.RequestStatus
import com.example.model.Tournament
import com.example.model.TournamentMode
import com.example.model.TournamentRegistration
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.model.WalletTxType
import com.example.model.WithdrawalRequest
import com.example.ui.components.CyberPanel
import com.example.ui.components.EmptyArenaState
import com.example.ui.components.NeonStatusBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ScreenshotUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    tournamentsState: UiState<List<Tournament>>,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    announcementsState: UiState<List<Announcement>>,
    depositRequestsState: UiState<List<DepositRequest>>,
    withdrawalRequestsState: UiState<List<WithdrawalRequest>>,
    allAdminTransactionsState: UiState<List<AdminWalletTransaction>>,
    selectedAdminTournamentId: String?,
    selectedRegistrationsState: UiState<List<TournamentRegistration>>,
    isBusy: Boolean,
    onSelectAdminTournament: (String?) -> Unit,
    onCreateTournament: (
        title: String, mode: TournamentMode, status: TournamentStatus, mapName: String,
        entryFeePkr: Long, prizePoolPkr: Long, firstPrizePkr: Long, secondPrizePkr: Long, thirdPrizePkr: Long,
        matchTime: String, totalSlots: Long, rules: String, roomId: String, roomPassword: String
    ) -> Unit,
    onUpdateTournament: (
        id: String, title: String, mode: TournamentMode, status: TournamentStatus, mapName: String,
        entryFeePkr: Long, prizePoolPkr: Long, firstPrizePkr: Long, secondPrizePkr: Long, thirdPrizePkr: Long,
        matchTime: String, totalSlots: Long, rules: String, roomId: String, roomPassword: String
    ) -> Unit,
    onDeleteTournament: (String) -> Unit,
    onPublishResult: (Tournament, TournamentRegistration, Long, Long, Long, Boolean, Long) -> Unit,
    onAdjustPlayerStats: (LeaderboardEntry, Long, Long, Long, Long) -> Unit,
    onCreateAnnouncement: (String, String, AnnouncementCategory) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onSeedStarters: () -> Unit,
    onReviewDeposit: (DepositRequest, Boolean, String) -> Unit,
    onReviewWithdrawal: (WithdrawalRequest, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var adminTab by remember { mutableIntStateOf(0) }
    val tournaments = (tournamentsState as? UiState.Success)?.data.orEmpty()
    val players = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val deposits = (depositRequestsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalRequestsState as? UiState.Success)?.data.orEmpty()
    val adminTransactions = (allAdminTransactionsState as? UiState.Success)?.data.orEmpty()

    val pendingWalletCount = deposits.count { it.status == RequestStatus.PENDING.raw } +
        withdrawals.count { it.status == RequestStatus.PENDING.raw }
    val walletTabLabel = if (pendingWalletCount > 0) "Wallet ($pendingWalletCount)" else "Wallet"
    val tabs = listOf("Tournaments", walletTabLabel, "Results", "Players", "Announcements")

    var editingTournament by remember { mutableStateOf<Tournament?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SectionHeader(
            title = "Admin Command Center",
            subtitle = "Manage tournaments, wallet requests, results & winners"
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { idx, title ->
                FilterChip(
                    selected = adminTab == idx,
                    onClick = { adminTab = idx },
                    label = {
                        Text(
                            text = title,
                            fontFamily = ChakraPetchFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldTrophy,
                        selectedLabelColor = PitchBlack,
                        containerColor = CyberCard,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (adminTab == idx) GoldTrophy else CyberBorder),
                    modifier = Modifier.testTag("admin_tab_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (adminTab) {
            0 -> AdminTournamentsTab(
                tournaments = tournaments,
                isBusy = isBusy,
                onOpenCreate = { showCreateDialog = true },
                onEdit = { editingTournament = it },
                onDelete = onDeleteTournament,
                onSeedStarters = onSeedStarters
            )

            1 -> AdminWalletVerificationTab(
                deposits = deposits,
                withdrawals = withdrawals,
                adminTransactionsState = allAdminTransactionsState,
                adminTransactions = adminTransactions,
                isBusy = isBusy,
                onReviewDeposit = onReviewDeposit,
                onReviewWithdrawal = onReviewWithdrawal
            )

            2 -> AdminResultsTab(
                tournaments = tournaments,
                selectedTournamentId = selectedAdminTournamentId,
                registrationsState = selectedRegistrationsState,
                isBusy = isBusy,
                onSelectTournament = onSelectAdminTournament,
                onPublishResult = onPublishResult
            )

            3 -> AdminPlayersTab(
                players = players,
                isBusy = isBusy,
                onAdjustStats = onAdjustPlayerStats
            )

            4 -> AdminAnnouncementsTab(
                announcements = announcements,
                isBusy = isBusy,
                onCreate = onCreateAnnouncement,
                onDelete = onDeleteAnnouncement
            )
        }
    }

    if (showCreateDialog) {
        TournamentEditorDialog(
            existing = null,
            onDismiss = { showCreateDialog = false },
            onSave = { title, mode, status, map, fee, pool, p1, p2, p3, time, slots, rules, roomId, roomPass ->
                onCreateTournament(title, mode, status, map, fee, pool, p1, p2, p3, time, slots, rules, roomId, roomPass)
                showCreateDialog = false
            }
        )
    }

    editingTournament?.let { t ->
        TournamentEditorDialog(
            existing = t,
            onDismiss = { editingTournament = null },
            onSave = { title, mode, status, map, fee, pool, p1, p2, p3, time, slots, rules, roomId, roomPass ->
                onUpdateTournament(t.id, title, mode, status, map, fee, pool, p1, p2, p3, time, slots, rules, roomId, roomPass)
                editingTournament = null
            }
        )
    }
}

@Composable
private fun AdminTournamentsTab(
    tournaments: List<Tournament>,
    isBusy: Boolean,
    onOpenCreate: () -> Unit,
    onEdit: (Tournament) -> Unit,
    onDelete: (String) -> Unit,
    onSeedStarters: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenCreate,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("admin_create_tournament_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CREATE TOURNAMENT", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
            }

            if (tournaments.isEmpty()) {
                OutlinedButton(
                    onClick = onSeedStarters,
                    enabled = !isBusy,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("admin_seed_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldTrophy)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Seed PK", color = GoldTrophy)
                }
            }
        }

        if (tournaments.isEmpty()) {
            EmptyArenaState(
                title = "No Tournaments Created",
                subtitle = "Create a new tournament or tap Seed PK to publish starter Free Fire events."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tournaments, key = { it.id }) { t ->
                    CyberPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                NeonStatusBadge(
                                    text = "${t.status.uppercase()} • ${t.mode}",
                                    color = NeonGreen
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(t.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "${t.mapName} • Fee ${t.entryFeePkr} PKR • Pool ${t.prizePoolPkr} PKR • ${t.registeredCount}/${t.totalSlots}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Row {
                                IconButton(onClick = { onEdit(t) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit tournament", tint = CyanAccent)
                                }
                                IconButton(onClick = { onDelete(t.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete tournament", tint = CrimsonLive)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWalletVerificationTab(
    deposits: List<DepositRequest>,
    withdrawals: List<WithdrawalRequest>,
    adminTransactionsState: UiState<List<AdminWalletTransaction>>,
    adminTransactions: List<AdminWalletTransaction>,
    isBusy: Boolean,
    onReviewDeposit: (DepositRequest, Boolean, String) -> Unit,
    onReviewWithdrawal: (WithdrawalRequest, Boolean, String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    var walletSubTab by remember { mutableIntStateOf(0) } // 0=Deposits, 1=Withdrawals, 2=All History
    var pendingOnly by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var txTypeFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Deposits, 2=Withdrawals, 3=Match Fees, 4=Prizes
    var previewScreenshot by remember { mutableStateOf<String?>(null) }
    var pendingDepositDecision by remember { mutableStateOf<Pair<DepositRequest, Boolean>?>(null) }
    var pendingWithdrawalDecision by remember { mutableStateOf<Pair<WithdrawalRequest, Boolean>?>(null) }

    val pendingDepCount = deposits.count { it.status == RequestStatus.PENDING.raw }
    val pendingWdCount = withdrawals.count { it.status == RequestStatus.PENDING.raw }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = walletSubTab == 0,
                onClick = { walletSubTab = 0 },
                label = { Text("Deposits ($pendingDepCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NeonGreen,
                    selectedLabelColor = PitchBlack
                ),
                modifier = Modifier.testTag("admin_wallet_deposits_tab")
            )
            FilterChip(
                selected = walletSubTab == 1,
                onClick = { walletSubTab = 1 },
                label = { Text("Withdrawals ($pendingWdCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanAccent,
                    selectedLabelColor = PitchBlack
                ),
                modifier = Modifier.testTag("admin_wallet_withdrawals_tab")
            )
            FilterChip(
                selected = walletSubTab == 2,
                onClick = { walletSubTab = 2 },
                label = { Text("All History (${adminTransactions.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldTrophy,
                    selectedLabelColor = PitchBlack
                ),
                modifier = Modifier.testTag("admin_wallet_history_tab")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                placeholder = {
                    Text(
                        when (walletSubTab) {
                            0 -> "Search TID, IGN, email..."
                            1 -> "Search account, IGN, email..."
                            else -> "Search IGN, email, TID, title..."
                        },
                        maxLines = 1
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("admin_wallet_search_input")
            )
            if (walletSubTab != 2) {
                FilterChip(
                    selected = pendingOnly,
                    onClick = { pendingOnly = !pendingOnly },
                    label = { Text(if (pendingOnly) "Pending Only" else "All Status") },
                    modifier = Modifier.testTag("admin_wallet_status_toggle")
                )
            }
        }

        val q = searchQuery.trim()
        when (walletSubTab) {
            0 -> {
                val shown = deposits.filter { dep ->
                    val statusOk = !pendingOnly || dep.status == RequestStatus.PENDING.raw
                    val queryOk = q.isBlank() ||
                        dep.transactionId.contains(q, ignoreCase = true) ||
                        dep.inGameName.contains(q, ignoreCase = true) ||
                        dep.userEmail.contains(q, ignoreCase = true) ||
                        dep.senderNumber.contains(q, ignoreCase = true)
                    statusOk && queryOk
                }
                if (shown.isEmpty()) {
                    EmptyArenaState(
                        title = if (pendingOnly && q.isBlank()) "No Pending Deposits" else "No Matching Deposits",
                        subtitle = "EasyPaisa and JazzCash deposit proofs submitted by players appear here."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shown, key = { it.id }) { dep ->
                            val status = RequestStatus.fromRaw(dep.status)
                            val method = PaymentMethod.fromRaw(dep.paymentMethod)
                            val bitmap = remember(dep.screenshotBase64) {
                                ScreenshotUtils.decodeBase64ToBitmap(dep.screenshotBase64)
                            }
                            CyberPanel(
                                borderColor = when (status) {
                                    RequestStatus.PENDING -> GoldTrophy.copy(alpha = 0.7f)
                                    RequestStatus.APPROVED -> NeonGreen.copy(alpha = 0.45f)
                                    RequestStatus.REJECTED -> CrimsonLive.copy(alpha = 0.45f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NeonStatusBadge(
                                        text = "${method.displayName} • ${status.label}",
                                        color = if (status == RequestStatus.APPROVED) NeonGreen else if (status == RequestStatus.REJECTED) CrimsonLive else GoldTrophy
                                    )
                                    Text(
                                        text = "+${dep.amountPkr} PKR",
                                        fontFamily = BebasNeueFontFamily,
                                        fontSize = 24.sp,
                                        color = NeonGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${dep.inGameName} (${dep.userEmail})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "TID: ${dep.transactionId} • To: ${dep.receiverAccountNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CyanAccent,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(dep.transactionId))
                                            Toast.makeText(context, "TID copied: ${dep.transactionId}", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ContentCopy,
                                            contentDescription = "Copy Transaction ID",
                                            tint = CyanAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = buildString {
                                        if (dep.senderNumber.isNotBlank()) append("Sender: ${dep.senderNumber} • ")
                                        append(formatAdminTimestamp(dep.createdAt?.toDate()))
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                if (dep.adminNote.isNotBlank()) {
                                    Text(
                                        text = "Admin Note: ${dep.adminNote}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldTrophy
                                    )
                                }
                                if (bitmap != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Deposit screenshot thumbnail",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                                            .clickable { previewScreenshot = dep.screenshotBase64 }
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { previewScreenshot = dep.screenshotBase64 },
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open Full Screenshot", fontSize = 12.sp)
                                    }
                                }
                                if (status == RequestStatus.PENDING) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { pendingDepositDecision = dep to false },
                                            enabled = !isBusy,
                                            border = BorderStroke(1.dp, CrimsonLive),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLive),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reject")
                                        }
                                        Button(
                                            onClick = { pendingDepositDecision = dep to true },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve & Credit", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                val shown = withdrawals.filter { wd ->
                    val statusOk = !pendingOnly || wd.status == RequestStatus.PENDING.raw
                    val queryOk = q.isBlank() ||
                        wd.accountNumber.contains(q, ignoreCase = true) ||
                        wd.accountTitle.contains(q, ignoreCase = true) ||
                        wd.inGameName.contains(q, ignoreCase = true) ||
                        wd.userEmail.contains(q, ignoreCase = true)
                    statusOk && queryOk
                }
                if (shown.isEmpty()) {
                    EmptyArenaState(
                        title = if (pendingOnly && q.isBlank()) "No Pending Withdrawals" else "No Matching Withdrawals",
                        subtitle = "Player EasyPaisa and JazzCash cashout requests appear here."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(shown, key = { it.id }) { wd ->
                            val status = RequestStatus.fromRaw(wd.status)
                            val method = PaymentMethod.fromRaw(wd.paymentMethod)
                            CyberPanel(
                                borderColor = when (status) {
                                    RequestStatus.PENDING -> CyanAccent.copy(alpha = 0.7f)
                                    RequestStatus.APPROVED -> NeonGreen.copy(alpha = 0.45f)
                                    RequestStatus.REJECTED -> CrimsonLive.copy(alpha = 0.45f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NeonStatusBadge(
                                        text = "${method.displayName} • ${status.label}",
                                        color = if (status == RequestStatus.APPROVED) NeonGreen else if (status == RequestStatus.REJECTED) CrimsonLive else CyanAccent
                                    )
                                    Text(
                                        text = "${wd.amountPkr} PKR",
                                        fontFamily = BebasNeueFontFamily,
                                        fontSize = 24.sp,
                                        color = GoldTrophy
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${wd.inGameName} (${wd.userEmail})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Account: ${wd.accountNumber} • Title: ${wd.accountTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanAccent
                                )
                                Text(
                                    text = formatAdminTimestamp(wd.createdAt?.toDate()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                if (wd.adminNote.isNotBlank()) {
                                    Text(
                                        text = "Admin Note: ${wd.adminNote}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GoldTrophy
                                    )
                                }
                                if (status == RequestStatus.PENDING) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { pendingWithdrawalDecision = wd to false },
                                            enabled = !isBusy,
                                            border = BorderStroke(1.dp, CrimsonLive),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonLive),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject & Refund")
                                        }
                                        Button(
                                            onClick = { pendingWithdrawalDecision = wd to true },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("Mark Paid", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                val filterLabels = listOf("All", "Deposits", "Withdrawals", "Match Fees", "Prizes")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = txTypeFilter == idx,
                            onClick = { txTypeFilter = idx },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                when (adminTransactionsState) {
                    is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                    is UiState.Error -> EmptyArenaState("Transaction History Error", adminTransactionsState.message)
                    is UiState.Success -> {
                        val filteredTx = adminTransactions.filter { tx ->
                            val typeOk = when (txTypeFilter) {
                                1 -> tx.type == WalletTxType.DEPOSIT.raw
                                2 -> tx.type == WalletTxType.WITHDRAWAL.raw || tx.type == WalletTxType.WITHDRAWAL_REFUND.raw
                                3 -> tx.type == WalletTxType.TOURNAMENT_ENTRY.raw
                                4 -> tx.type == WalletTxType.PRIZE_PAYOUT.raw
                                else -> true
                            }
                            val queryOk = q.isBlank() ||
                                tx.inGameName.contains(q, ignoreCase = true) ||
                                tx.userEmail.contains(q, ignoreCase = true) ||
                                tx.title.contains(q, ignoreCase = true) ||
                                tx.subtitle.contains(q, ignoreCase = true)
                            typeOk && queryOk
                        }
                        if (filteredTx.isEmpty()) {
                            EmptyArenaState(
                                title = "No Wallet Transactions",
                                subtitle = "Completed deposits, withdrawals, match entry fees, and prize payouts across all players appear here.",
                                icon = Icons.Filled.ReceiptLong
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredTx, key = { it.id }) { tx ->
                                    val isCredit = tx.direction == "credit"
                                    val accent = if (isCredit) NeonGreen else CrimsonLive
                                    val txType = WalletTxType.fromRaw(tx.type)
                                    CyberPanel(borderColor = accent.copy(alpha = 0.35f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            NeonStatusBadge(text = "${txType.label} • ${tx.status.uppercase()}", color = accent)
                                            Text(
                                                text = "${if (isCredit) "+" else "-"}${tx.amountPkr} PKR",
                                                fontFamily = BebasNeueFontFamily,
                                                fontSize = 22.sp,
                                                color = accent
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "${tx.inGameName.ifBlank { tx.userId }} (${tx.userEmail})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = tx.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = CyanAccent
                                        )
                                        Text(
                                            text = "${tx.subtitle} • Bal: ${tx.balanceAfterPkr} PKR • ${formatAdminTimestamp(tx.createdAt?.toDate())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewScreenshot?.let { b64 ->
        ScreenshotViewerDialog(base64 = b64, onDismiss = { previewScreenshot = null })
    }

    pendingDepositDecision?.let { (dep, approve) ->
        AdminNoteConfirmDialog(
            title = if (approve) "Approve & Credit ${dep.amountPkr} PKR?" else "Reject Deposit Request?",
            message = "Player: ${dep.inGameName} • TID: ${dep.transactionId}",
            confirmLabel = if (approve) "Approve & Credit" else "Reject",
            confirmColor = if (approve) NeonGreen else CrimsonLive,
            defaultNote = if (approve) "Verified" else "Transaction ID or screenshot not verified",
            onDismiss = { pendingDepositDecision = null },
            onConfirm = { note ->
                onReviewDeposit(dep, approve, note)
                pendingDepositDecision = null
            }
        )
    }

    pendingWithdrawalDecision?.let { (wd, approve) ->
        AdminNoteConfirmDialog(
            title = if (approve) "Mark ${wd.amountPkr} PKR Paid?" else "Reject & Refund ${wd.amountPkr} PKR?",
            message = "To: ${wd.accountNumber} (${wd.accountTitle})",
            confirmLabel = if (approve) "Confirm Paid" else "Reject & Refund",
            confirmColor = if (approve) NeonGreen else CrimsonLive,
            defaultNote = if (approve) "Payout sent" else "Invalid account details",
            onDismiss = { pendingWithdrawalDecision = null },
            onConfirm = { note ->
                onReviewWithdrawal(wd, approve, note)
                pendingWithdrawalDecision = null
            }
        )
    }
}

@Composable
private fun AdminNoteConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: androidx.compose.ui.graphics.Color,
    defaultNote: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf(defaultNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text(title, color = confirmColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Admin Note / Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note) },
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = PitchBlack)
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AdminResultsTab(
    tournaments: List<Tournament>,
    selectedTournamentId: String?,
    registrationsState: UiState<List<TournamentRegistration>>,
    isBusy: Boolean,
    onSelectTournament: (String?) -> Unit,
    onPublishResult: (Tournament, TournamentRegistration, Long, Long, Long, Boolean, Long) -> Unit
) {
    val activeTournament = tournaments.firstOrNull { it.id == selectedTournamentId } ?: tournaments.firstOrNull()
    LaunchedEffect(activeTournament?.id) {
        if (activeTournament != null && selectedTournamentId != activeTournament.id) {
            onSelectTournament(activeTournament.id)
        }
    }

    if (tournaments.isEmpty()) {
        EmptyArenaState("No Tournaments", "Create a tournament first to publish winners and points.")
        return
    }

    var scoringTarget by remember { mutableStateOf<TournamentRegistration?>(null) }
    val registrations = (registrationsState as? UiState.Success)?.data.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tournaments.forEach { t ->
                val selected = activeTournament?.id == t.id
                FilterChip(
                    selected = selected,
                    onClick = { onSelectTournament(t.id) },
                    label = { Text(t.title, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreen,
                        selectedLabelColor = PitchBlack
                    )
                )
            }
        }

        if (registrations.isEmpty()) {
            EmptyArenaState(
                title = "No Registered Players",
                subtitle = "Players who join ${activeTournament?.title ?: "this tournament"} will appear here for scoring."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(registrations, key = { it.userId }) { reg ->
                    CyberPanel {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Slot #${reg.slotNumber} • ${reg.inGameName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${reg.teamName} • Placement #${reg.placement} • ${reg.totalPoints} PTS",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = { scoringTarget = reg },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(containerColor = GoldTrophy, contentColor = PitchBlack),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Score")
                            }
                        }
                    }
                }
            }
        }
    }

    scoringTarget?.let { reg ->
        if (activeTournament != null) {
            PublishScoreDialog(
                tournament = activeTournament,
                registration = reg,
                onDismiss = { scoringTarget = null },
                onSubmit = { matchPts, killPts, placement, isWinner, prizePkr ->
                    onPublishResult(activeTournament, reg, matchPts, killPts, placement, isWinner, prizePkr)
                    scoringTarget = null
                }
            )
        }
    }
}

@Composable
private fun AdminPlayersTab(
    players: List<LeaderboardEntry>,
    isBusy: Boolean,
    onAdjustStats: (LeaderboardEntry, Long, Long, Long, Long) -> Unit
) {
    if (players.isEmpty()) {
        EmptyArenaState("No Players Registered", "Signed-up players appear here.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(players, key = { it.userId }) { entry ->
            CyberPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.inGameName, style = MaterialTheme.typography.titleMedium, color = NeonGreen)
                        Text(
                            text = "${entry.teamName} • UID ${entry.freeFireUid}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${entry.matchesPlayed}M • ${entry.totalWins}W • ${entry.totalPoints} PTS",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldTrophy
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { onAdjustStats(entry, 1L, 0L, 10L, 5L) },
                            enabled = !isBusy,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("+15 Pts", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onAdjustStats(entry, 1L, 1L, 25L, 10L) },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldTrophy, contentColor = PitchBlack),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text("+Win", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAnnouncementsTab(
    announcements: List<Announcement>,
    isBusy: Boolean,
    onCreate: (String, String, AnnouncementCategory) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(AnnouncementCategory.TOURNAMENT) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            CyberPanel(borderColor = CyanAccent.copy(alpha = 0.5f)) {
                Text("NEW ANNOUNCEMENT", style = MaterialTheme.typography.titleMedium, color = CyanAccent)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AnnouncementCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (title.trim().length >= 3 && message.trim().length >= 5) {
                            onCreate(title, message, category)
                            title = ""
                            message = ""
                        }
                    },
                    enabled = !isBusy && title.trim().length >= 3 && message.trim().length >= 5,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BROADCAST ANNOUNCEMENT", fontFamily = RussoOneFontFamily)
                }
            }
        }

        items(announcements, key = { it.id }) { ann ->
            CyberPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        NeonStatusBadge(text = ann.category, color = CyanAccent)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ann.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(ann.message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    IconButton(onClick = { onDelete(ann.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete announcement", tint = CrimsonLive)
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentEditorDialog(
    existing: Tournament?,
    onDismiss: () -> Unit,
    onSave: (
        title: String, mode: TournamentMode, status: TournamentStatus, mapName: String,
        entryFeePkr: Long, prizePoolPkr: Long, firstPrizePkr: Long, secondPrizePkr: Long, thirdPrizePkr: Long,
        matchTime: String, totalSlots: Long, rules: String, roomId: String, roomPassword: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var mode by remember { mutableStateOf(TournamentMode.fromRaw(existing?.mode ?: "Squad")) }
    var status by remember { mutableStateOf(TournamentStatus.fromRaw(existing?.status ?: "upcoming")) }
    var mapName by remember { mutableStateOf(existing?.mapName ?: "Bermuda") }
    var feeText by remember { mutableStateOf((existing?.entryFeePkr ?: 50L).toString()) }
    var prizePoolText by remember { mutableStateOf((existing?.prizePoolPkr ?: 1750L).toString()) }
    var p1Text by remember { mutableStateOf((existing?.firstPrizePkr ?: 1000L).toString()) }
    var p2Text by remember { mutableStateOf((existing?.secondPrizePkr ?: 500L).toString()) }
    var p3Text by remember { mutableStateOf((existing?.thirdPrizePkr ?: 250L).toString()) }
    var matchTime by remember { mutableStateOf(existing?.matchTime ?: "Tonight • 9:00 PM PKT") }
    var slotsText by remember { mutableStateOf((existing?.totalSlots ?: 48L).toString()) }
    var rules by remember { mutableStateOf(existing?.rules ?: "Mobile only. No hacks or teaming. Join room 10 mins early.") }
    var roomId by remember { mutableStateOf(existing?.roomId ?: "") }
    var roomPass by remember { mutableStateOf(existing?.roomPassword ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = if (existing == null) "Create Tournament" else "Edit Tournament",
                style = MaterialTheme.typography.titleLarge,
                color = NeonGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tournament Name") }, singleLine = true)
                Text("Mode", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentMode.entries.forEach { m ->
                        FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m.label) })
                    }
                }
                Text("Status", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TournamentStatus.entries.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                    }
                }
                OutlinedTextField(value = mapName, onValueChange = { mapName = it }, label = { Text("Map (e.g. Bermuda)") }, singleLine = true)
                OutlinedTextField(value = matchTime, onValueChange = { matchTime = it }, label = { Text("Match Time") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = feeText,
                        onValueChange = { feeText = it.filter { c -> c.isDigit() } },
                        label = { Text("Fee (PKR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = slotsText,
                        onValueChange = { slotsText = it.filter { c -> c.isDigit() } },
                        label = { Text("Slots") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = prizePoolText,
                    onValueChange = { prizePoolText = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Prize Pool (PKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = p1Text,
                        onValueChange = { p1Text = it.filter { c -> c.isDigit() } },
                        label = { Text("1st") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = p2Text,
                        onValueChange = { p2Text = it.filter { c -> c.isDigit() } },
                        label = { Text("2nd") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = p3Text,
                        onValueChange = { p3Text = it.filter { c -> c.isDigit() } },
                        label = { Text("3rd") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = roomId, onValueChange = { roomId = it }, label = { Text("Room ID") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = roomPass, onValueChange = { roomPass = it }, label = { Text("Password") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = rules, onValueChange = { rules = it }, label = { Text("Rules") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        mode,
                        status,
                        mapName,
                        feeText.toLongOrNull() ?: 0L,
                        prizePoolText.toLongOrNull() ?: 0L,
                        p1Text.toLongOrNull() ?: 0L,
                        p2Text.toLongOrNull() ?: 0L,
                        p3Text.toLongOrNull() ?: 0L,
                        matchTime,
                        (slotsText.toLongOrNull() ?: 48L).coerceIn(2L, 100L),
                        rules,
                        roomId,
                        roomPass
                    )
                },
                enabled = title.trim().length >= 3 && mapName.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PublishScoreDialog(
    tournament: Tournament,
    registration: TournamentRegistration,
    onDismiss: () -> Unit,
    onSubmit: (matchPts: Long, killPts: Long, placement: Long, isWinner: Boolean, prizePayoutPkr: Long) -> Unit
) {
    var matchPts by remember { mutableStateOf(registration.matchPoints.toString()) }
    var killPts by remember { mutableStateOf(registration.killPoints.toString()) }
    var placement by remember { mutableStateOf(if (registration.placement > 0) registration.placement.toString() else "1") }
    var prizeText by remember { mutableStateOf("0") }
    var markAsWinner by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text("Publish Score: ${registration.inGameName}", color = GoldTrophy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = placement,
                    onValueChange = { placement = it.filter { c -> c.isDigit() } },
                    label = { Text("Placement (#1, #2...)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = matchPts,
                    onValueChange = { matchPts = it.filter { c -> c.isDigit() } },
                    label = { Text("Match Placement Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = killPts,
                    onValueChange = { killPts = it.filter { c -> c.isDigit() } },
                    label = { Text("Kill Points") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = prizeText,
                    onValueChange = { prizeText = it.filter { c -> c.isDigit() } },
                    label = { Text("Credit Wallet Prize (PKR)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tournament.firstPrizePkr > 0L) {
                        FilterChip(
                            selected = prizeText == tournament.firstPrizePkr.toString(),
                            onClick = { prizeText = tournament.firstPrizePkr.toString(); markAsWinner = true },
                            label = { Text("1st: ${tournament.firstPrizePkr}") }
                        )
                    }
                    if (tournament.secondPrizePkr > 0L) {
                        FilterChip(
                            selected = prizeText == tournament.secondPrizePkr.toString(),
                            onClick = { prizeText = tournament.secondPrizePkr.toString() },
                            label = { Text("2nd: ${tournament.secondPrizePkr}") }
                        )
                    }
                    if (tournament.thirdPrizePkr > 0L) {
                        FilterChip(
                            selected = prizeText == tournament.thirdPrizePkr.toString(),
                            onClick = { prizeText = tournament.thirdPrizePkr.toString() },
                            label = { Text("3rd: ${tournament.thirdPrizePkr}") }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = markAsWinner, onCheckedChange = { markAsWinner = it })
                    Text("Publish as #1 Winner & Complete Match", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        matchPts.toLongOrNull() ?: 0L,
                        killPts.toLongOrNull() ?: 0L,
                        placement.toLongOrNull() ?: 1L,
                        markAsWinner,
                        prizeText.toLongOrNull() ?: 0L
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldTrophy, contentColor = PitchBlack)
            ) {
                Text("Publish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatAdminTimestamp(date: Date?): String {
    if (date == null) return "Just now"
    return SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).format(date)
}
