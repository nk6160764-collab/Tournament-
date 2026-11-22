package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.model.DepositRequest
import com.example.model.PaymentMethod
import com.example.model.PlayerProfile
import com.example.model.RequestStatus
import com.example.model.UiState
import com.example.model.WalletLimits
import com.example.model.WalletTransaction
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
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ScreenshotUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class WalletQuickAction {
    ADD_MONEY,
    WITHDRAW,
    TOURNAMENT_PAYMENTS
}

@Composable
fun WalletScreen(
    profile: PlayerProfile,
    transactionsState: UiState<List<WalletTransaction>>,
    depositRequestsState: UiState<List<DepositRequest>>,
    withdrawalRequestsState: UiState<List<WithdrawalRequest>>,
    initialAction: WalletQuickAction?,
    isBusy: Boolean,
    onConsumeInitialAction: () -> Unit,
    onBack: () -> Unit,
    onSubmitDeposit: (
        amountPkr: Long,
        method: PaymentMethod,
        transactionId: String,
        senderNumber: String,
        screenshotBase64: String
    ) -> Unit,
    onSubmitWithdrawal: (
        amountPkr: Long,
        method: PaymentMethod,
        accountNumber: String,
        accountTitle: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var txFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Tournament Fees, 2=Deposits & Prizes, 3=Withdrawals
    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var previewScreenshotBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialAction) {
        when (initialAction) {
            WalletQuickAction.ADD_MONEY -> {
                showAddMoneyDialog = true
                onConsumeInitialAction()
            }
            WalletQuickAction.WITHDRAW -> {
                showWithdrawDialog = true
                onConsumeInitialAction()
            }
            WalletQuickAction.TOURNAMENT_PAYMENTS -> {
                selectedTab = 0
                txFilter = 1
                onConsumeInitialAction()
            }
            null -> Unit
        }
    }

    val tabs = listOf("Transactions", "Deposits", "Withdrawals", "Pay Methods")
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val deposits = (depositRequestsState as? UiState.Success)?.data.orEmpty()
    val withdrawals = (withdrawalRequestsState as? UiState.Success)?.data.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
            .navigationBarsPadding()
    ) {
        // Top Bar
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
                    .testTag("wallet_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonGreen
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GAMERZONE WALLET",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonGreen
                )
                Text(
                    text = "My Wallet & Payments",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }
            NeonStatusBadge(text = profile.inGameName, color = CyanAccent)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Available Balance Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NeonGreen.copy(alpha = 0.16f),
                                CyberCardElevated,
                                CyanAccent.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(1.5.dp, NeonGreen.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
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
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AVAILABLE BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen
                            )
                        }
                        val pendingDeps = deposits.count { it.status == RequestStatus.PENDING.raw }
                        if (pendingDeps > 0) {
                            NeonStatusBadge(text = "$pendingDeps PENDING DEPOSIT", color = GoldTrophy)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${profile.walletBalancePkr} PKR",
                        fontFamily = BebasNeueFontFamily,
                        fontSize = 42.sp,
                        color = TextPrimary,
                        modifier = Modifier.testTag("wallet_balance_value")
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showAddMoneyDialog = true },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = PitchBlack
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("wallet_add_money_button")
                        ) {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ADD MONEY", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showWithdrawDialog = true },
                            enabled = !isBusy,
                            border = BorderStroke(1.2.dp, CyanAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("wallet_withdraw_button")
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WITHDRAW", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        label = {
                            Text(
                                text = label,
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
                        border = BorderStroke(1.dp, if (selectedTab == idx) NeonGreen else CyberBorder),
                        modifier = Modifier.testTag("wallet_tab_$idx")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> {
                    val txFilters = listOf("All", "Tournament Fees", "Deposits & Prizes", "Withdrawals")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        txFilters.forEachIndexed { idx, title ->
                            FilterChip(
                                selected = txFilter == idx,
                                onClick = { txFilter = idx },
                                label = { Text(title, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanAccent,
                                    selectedLabelColor = PitchBlack,
                                    containerColor = CyberCard,
                                    labelColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("wallet_tx_filter_$idx")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredTx = transactions.filter { tx ->
                        when (txFilter) {
                            1 -> tx.type == WalletTxType.TOURNAMENT_ENTRY.raw
                            2 -> tx.type == WalletTxType.DEPOSIT.raw || tx.type == WalletTxType.PRIZE_PAYOUT.raw
                            3 -> tx.type == WalletTxType.WITHDRAWAL.raw || tx.type == WalletTxType.WITHDRAWAL_REFUND.raw
                            else -> true
                        }
                    }
                    TransactionHistoryList(
                        state = transactionsState,
                        items = filteredTx
                    )
                }

                1 -> DepositHistoryList(
                    state = depositRequestsState,
                    items = deposits,
                    onViewScreenshot = { previewScreenshotBase64 = it }
                )

                2 -> WithdrawalHistoryList(
                    state = withdrawalRequestsState,
                    items = withdrawals
                )

                3 -> PaymentMethodsInfoList(
                    onDepositWithMethod = { showAddMoneyDialog = true }
                )
            }
        }
    }

    if (showAddMoneyDialog) {
        AddMoneyDialog(
            isBusy = isBusy,
            onDismiss = { showAddMoneyDialog = false },
            onSubmit = { amount, method, tid, sender, base64 ->
                onSubmitDeposit(amount, method, tid, sender, base64)
                showAddMoneyDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        WithdrawMoneyDialog(
            availableBalance = profile.walletBalancePkr,
            defaultAccountName = profile.displayName.ifBlank { profile.inGameName },
            defaultPhone = profile.whatsappNumber,
            isBusy = isBusy,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { amount, method, accNum, accTitle ->
                onSubmitWithdrawal(amount, method, accNum, accTitle)
                showWithdrawDialog = false
            }
        )
    }

    previewScreenshotBase64?.let { b64 ->
        ScreenshotViewerDialog(
            base64 = b64,
            onDismiss = { previewScreenshotBase64 = null }
        )
    }
}

@Composable
private fun TransactionHistoryList(
    state: UiState<List<WalletTransaction>>,
    items: List<WalletTransaction>
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        is UiState.Error -> EmptyArenaState("Transaction Error", state.message)
        is UiState.Success -> {
            if (items.isEmpty()) {
                EmptyArenaState(
                    title = "No Transactions Yet",
                    subtitle = "Deposits, tournament entry fees, prize winnings, and withdrawals appear here.",
                    icon = Icons.Filled.ReceiptLong
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { tx ->
                        val isCredit = tx.direction == "credit"
                        val accent = if (isCredit) NeonGreen else CrimsonLive
                        CyberPanel(borderColor = accent.copy(alpha = 0.35f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.copy(alpha = 0.14f))
                                    ) {
                                        Icon(
                                            imageVector = if (isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = tx.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = tx.subtitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = formatTimestamp(tx.createdAt?.toDate()),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (isCredit) "+" else "-"}${tx.amountPkr} PKR",
                                        fontFamily = BebasNeueFontFamily,
                                        fontSize = 20.sp,
                                        color = accent
                                    )
                                    Text(
                                        text = "Bal: ${tx.balanceAfterPkr} PKR",
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

@Composable
private fun DepositHistoryList(
    state: UiState<List<DepositRequest>>,
    items: List<DepositRequest>,
    onViewScreenshot: (String) -> Unit
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        is UiState.Error -> EmptyArenaState("Deposit History Error", state.message)
        is UiState.Success -> {
            if (items.isEmpty()) {
                EmptyArenaState(
                    title = "No Deposit Requests",
                    subtitle = "Tap Add Money to deposit via EasyPaisa or JazzCash."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { dep ->
                        val status = RequestStatus.fromRaw(dep.status)
                        val method = PaymentMethod.fromRaw(dep.paymentMethod)
                        val color = statusColor(status)
                        CyberPanel(borderColor = color.copy(alpha = 0.45f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeonStatusBadge(text = "${method.displayName} • ${status.label}", color = color)
                                Text(
                                    text = "+${dep.amountPkr} PKR",
                                    fontFamily = BebasNeueFontFamily,
                                    fontSize = 22.sp,
                                    color = NeonGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Transaction ID: ${dep.transactionId}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Sent to: ${dep.receiverAccountNumber} • ${formatTimestamp(dep.createdAt?.toDate())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            if (dep.adminNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Admin Note: ${dep.adminNote}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CyanAccent
                                )
                            }
                            if (dep.screenshotBase64.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { onViewScreenshot(dep.screenshotBase64) },
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("View Uploaded Screenshot", fontSize = 12.sp)
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
private fun WithdrawalHistoryList(
    state: UiState<List<WithdrawalRequest>>,
    items: List<WithdrawalRequest>
) {
    when (state) {
        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonGreen)
        }
        is UiState.Error -> EmptyArenaState("Withdrawal History Error", state.message)
        is UiState.Success -> {
            if (items.isEmpty()) {
                EmptyArenaState(
                    title = "No Withdrawals Yet",
                    subtitle = "Your EasyPaisa & JazzCash payout requests will appear here."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { wd ->
                        val status = RequestStatus.fromRaw(wd.status)
                        val method = PaymentMethod.fromRaw(wd.paymentMethod)
                        val color = statusColor(status)
                        CyberPanel(borderColor = color.copy(alpha = 0.45f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NeonStatusBadge(text = "${method.displayName} • ${status.label}", color = color)
                                Text(
                                    text = "${wd.amountPkr} PKR",
                                    fontFamily = BebasNeueFontFamily,
                                    fontSize = 22.sp,
                                    color = CyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Account: ${wd.accountNumber} (${wd.accountTitle})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = formatTimestamp(wd.createdAt?.toDate()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            if (wd.adminNote.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Admin Note: ${wd.adminNote}",
                                    style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun PaymentMethodsInfoList(onDepositWithMethod: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Official Payment Accounts",
                subtitle = "Send money to either account, then submit TID & screenshot"
            )
        }
        items(PaymentMethod.entries) { method ->
            val accent = if (method == PaymentMethod.EASYPAISA) NeonGreen else GoldTrophy
            CyberPanel(borderColor = accent.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        NeonStatusBadge(text = method.displayName, color = accent)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Account Number",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = method.accountNumber,
                            fontFamily = BebasNeueFontFamily,
                            fontSize = 28.sp,
                            letterSpacing = 1.5.sp,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(method.accountNumber))
                                Toast.makeText(context, "${method.displayName} number copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                        Button(
                            onClick = onDepositWithMethod,
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = PitchBlack)
                        ) {
                            Text("Deposit", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMoneyDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        amountPkr: Long,
        method: PaymentMethod,
        transactionId: String,
        senderNumber: String,
        screenshotBase64: String
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedMethod by remember { mutableStateOf(PaymentMethod.EASYPAISA) }
    var amountText by remember { mutableStateOf("100") }
    var transactionId by remember { mutableStateOf("") }
    var senderNumber by remember { mutableStateOf("") }
    var screenshotBase64 by remember { mutableStateOf<String?>(null) }
    var isEncoding by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val previewBitmap = remember(screenshotBase64) {
        screenshotBase64?.let { ScreenshotUtils.decodeBase64ToBitmap(it) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            localError = null
            isEncoding = true
            scope.launch {
                val res = ScreenshotUtils.encodeUriToBase64(context, uri)
                isEncoding = false
                res.onSuccess { b64 ->
                    screenshotBase64 = b64
                }.onFailure { err ->
                    localError = err.localizedMessage ?: "Failed to read screenshot."
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "ADD MONEY TO WALLET",
                fontFamily = RussoOneFontFamily,
                fontSize = 18.sp,
                color = NeonGreen
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("1. Select Payment Method", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val selected = selectedMethod == method
                        val accent = if (method == PaymentMethod.EASYPAISA) NeonGreen else GoldTrophy
                        Surface(
                            onClick = { selectedMethod = method },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) accent.copy(alpha = 0.16f) else CyberCard,
                            border = BorderStroke(1.5.dp, if (selected) accent else CyberBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("deposit_method_${method.raw}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = method.displayName,
                                    fontFamily = ChakraPetchFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) accent else TextPrimary
                                )
                                Text(
                                    text = method.accountNumber,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Payment Account Number Card
                CyberPanel(borderColor = NeonGreen.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SEND TO ${selectedMethod.displayName.uppercase()} ACCOUNT",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen
                            )
                            Text(
                                text = selectedMethod.accountNumber,
                                fontFamily = BebasNeueFontFamily,
                                fontSize = 26.sp,
                                color = TextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(selectedMethod.accountNumber))
                                Toast.makeText(context, "Account number copied!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Account Number", tint = NeonGreen)
                        }
                    }
                }

                // Quick Amount Chips + Field (Min 100 PKR)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("100", "250", "500", "1000").forEach { preset ->
                        FilterChip(
                            selected = amountText == preset,
                            onClick = { amountText = preset },
                            label = { Text("$preset PKR") }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Deposit Amount (PKR) *") },
                    supportingText = {
                        Text("Minimum deposit: ${WalletLimits.MIN_DEPOSIT_PKR} PKR • Credited after admin approval")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_amount_input")
                )

                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it.take(64) },
                    label = { Text("Transaction ID (TID) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_tid_input")
                )

                OutlinedTextField(
                    value = senderNumber,
                    onValueChange = { senderNumber = it.take(25) },
                    label = { Text("Your Sender Mobile Number (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deposit_sender_input")
                )

                // Zero-Permission Photo Picker Upload Button
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isEncoding,
                    border = BorderStroke(1.dp, if (previewBitmap != null) NeonGreen else CyanAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("deposit_pick_screenshot_button")
                ) {
                    if (isEncoding) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = NeonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing Screenshot...")
                    } else {
                        Icon(
                            imageVector = if (previewBitmap != null) Icons.Filled.CheckCircle else Icons.Filled.UploadFile,
                            contentDescription = null,
                            tint = if (previewBitmap != null) NeonGreen else CyanAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (previewBitmap != null) "Screenshot Attached (Tap to Change)" else "Upload Payment Screenshot *",
                            color = if (previewBitmap != null) NeonGreen else CyanAccent
                        )
                    }
                }

                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Selected payment screenshot preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    )
                }

                if (localError != null) {
                    Text(
                        text = localError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonLive
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    val cleanTid = transactionId.trim()
                    val b64 = screenshotBase64.orEmpty()
                    when {
                        amt < WalletLimits.MIN_DEPOSIT_PKR ->
                            localError = "Minimum deposit is ${WalletLimits.MIN_DEPOSIT_PKR} PKR."
                        amt > WalletLimits.MAX_DEPOSIT_PKR ->
                            localError = "Maximum single deposit is ${WalletLimits.MAX_DEPOSIT_PKR} PKR."
                        cleanTid.length < 4 ->
                            localError = "Enter a valid Transaction ID (min 4 characters)."
                        b64.length < 20 ->
                            localError = "Please upload your payment screenshot."
                        else -> {
                            localError = null
                            onSubmit(amt, selectedMethod, cleanTid, senderNumber.trim(), b64)
                        }
                    }
                },
                enabled = !isBusy && !isEncoding,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = PitchBlack),
                modifier = Modifier.testTag("deposit_submit_button")
            ) {
                Text("Submit Request", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WithdrawMoneyDialog(
    availableBalance: Long,
    defaultAccountName: String,
    defaultPhone: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        amountPkr: Long,
        method: PaymentMethod,
        accountNumber: String,
        accountTitle: String
    ) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.EASYPAISA) }
    var amountText by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf(defaultPhone) }
    var accountTitle by remember { mutableStateOf(defaultAccountName) }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = {
            Text(
                text = "WITHDRAW MONEY",
                fontFamily = RussoOneFontFamily,
                fontSize = 18.sp,
                color = CyanAccent
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Available Balance: $availableBalance PKR (Min: ${WalletLimits.MIN_WITHDRAWAL_PKR} PKR)",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val selected = selectedMethod == method
                        FilterChip(
                            selected = selected,
                            onClick = { selectedMethod = method },
                            label = { Text(method.displayName) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("withdraw_method_${method.raw}")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(200L, 500L, 1000L).filter { it <= availableBalance }.forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString() },
                            label = { Text("$preset") }
                        )
                    }
                    if (availableBalance >= WalletLimits.MIN_WITHDRAWAL_PKR) {
                        FilterChip(
                            selected = amountText == availableBalance.toString(),
                            onClick = { amountText = availableBalance.toString() },
                            label = { Text("Max") }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                    label = { Text("Withdraw Amount (PKR) *") },
                    supportingText = {
                        Text("Minimum withdrawal: ${WalletLimits.MIN_WITHDRAWAL_PKR} PKR")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input")
                )

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.take(20) },
                    label = { Text("Your ${selectedMethod.displayName} Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_account_input")
                )

                OutlinedTextField(
                    value = accountTitle,
                    onValueChange = { accountTitle = it.take(60) },
                    label = { Text("Account Holder Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_title_input")
                )

                if (localError != null) {
                    Text(
                        text = localError ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonLive
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    val cleanAcc = accountNumber.trim()
                    val cleanTitle = accountTitle.trim()
                    when {
                        amt < WalletLimits.MIN_WITHDRAWAL_PKR ->
                            localError = "Minimum withdrawal amount is ${WalletLimits.MIN_WITHDRAWAL_PKR} PKR."
                        amt > WalletLimits.MAX_WITHDRAWAL_PKR ->
                            localError = "Maximum single withdrawal is ${WalletLimits.MAX_WITHDRAWAL_PKR} PKR."
                        amt > availableBalance ->
                            localError = "Insufficient balance ($availableBalance PKR available)."
                        cleanAcc.length < 7 ->
                            localError = "Enter a valid mobile account number."
                        cleanTitle.length < 2 ->
                            localError = "Enter the account holder's name."
                        else -> {
                            localError = null
                            onSubmit(amt, selectedMethod, cleanAcc, cleanTitle)
                        }
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = PitchBlack),
                modifier = Modifier.testTag("withdraw_submit_button")
            ) {
                Text("Request Withdraw", fontFamily = RussoOneFontFamily, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ScreenshotViewerDialog(
    base64: String,
    onDismiss: () -> Unit
) {
    val bitmap = remember(base64) { ScreenshotUtils.decodeBase64ToBitmap(base64) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberSurface,
        title = { Text("Payment Screenshot", color = NeonGreen) },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Uploaded payment proof screenshot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text("Unable to decode screenshot preview.", color = CrimsonLive)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun statusColor(status: RequestStatus): Color = when (status) {
    RequestStatus.PENDING -> GoldTrophy
    RequestStatus.APPROVED -> NeonGreen
    RequestStatus.REJECTED -> CrimsonLive
}

private fun formatTimestamp(date: Date?): String {
    if (date == null) return "Just now"
    return SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).format(date)
}
