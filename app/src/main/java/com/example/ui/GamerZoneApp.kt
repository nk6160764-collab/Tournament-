package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOutUser
import com.example.data.GamerZoneRepository
import com.example.model.TournamentMode
import com.example.model.UiState
import com.example.ui.components.EmptyArenaState
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CompleteProfileScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.PlayerDashboardScreen
import com.example.ui.screens.TournamentDetailScreen
import com.example.ui.screens.TournamentsScreen
import com.example.ui.screens.WalletQuickAction
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.BebasNeueFontFamily
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CyberCard
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.DarkEmeraldContainer
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GamerZoneViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

private enum class AppDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Filled.Home),
    TOURNAMENTS("Matches", Icons.Filled.EmojiEvents),
    LEADERBOARD("Ranks", Icons.Filled.Leaderboard),
    PLAYER("Player", Icons.Filled.Person),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings)
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth
) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1100)
        showSplash = false
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    if (showSplash) {
        GamerZoneSplashScreen()
        return
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = auth.currentUser }
        )
    } else {
        key(user.uid) {
            AuthenticatedArenaShell(
                currentUserId = user.uid,
                initialDisplayName = user.displayName.orEmpty(),
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun GamerZoneSplashScreen() {
    val logoScale = remember { Animatable(0.86f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 450)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        NeonGreen.copy(alpha = 0.14f),
                        PitchBlack,
                        PitchBlack
                    )
                )
            )
            .testTag("gamerzone_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .alpha(contentAlpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .scale(logoScale.value)
                    .clip(RoundedCornerShape(28.dp))
                    .border(2.dp, NeonGreen.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_gamerzone_icon),
                    contentDescription = stringResource(id = R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = stringResource(id = R.string.brand_title),
                fontFamily = RussoOneFontFamily,
                fontSize = 30.sp,
                letterSpacing = 2.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "FREE FIRE ESPORTS ARENA",
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 2.5.sp,
                color = NeonGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            LinearProgressIndicator(
                color = NeonGreen,
                trackColor = CyberCard,
                modifier = Modifier
                    .width(180.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun AuthenticatedArenaShell(
    currentUserId: String,
    initialDisplayName: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GamerZoneViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                GamerZoneViewModel(GamerZoneRepository(db), currentUserId)
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournamentsState.collectAsStateWithLifecycle()
    val myRegistrationsState by viewModel.myRegistrationsState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
    val teamRankings by viewModel.teamRankings.collectAsStateWithLifecycle()
    val selectedTournamentId by viewModel.selectedTournamentId.collectAsStateWithLifecycle()
    val selectedRegistrationsState by viewModel.selectedTournamentRegistrations.collectAsStateWithLifecycle()
    val myWalletTransactionsState by viewModel.myWalletTransactionsState.collectAsStateWithLifecycle()
    val myDepositRequestsState by viewModel.myDepositRequestsState.collectAsStateWithLifecycle()
    val myWithdrawalRequestsState by viewModel.myWithdrawalRequestsState.collectAsStateWithLifecycle()
    val allDepositRequestsState by viewModel.allDepositRequestsState.collectAsStateWithLifecycle()
    val allWithdrawalRequestsState by viewModel.allWithdrawalRequestsState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val doSignOut: () -> Unit = {
        signOutUser(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = onSignedOut
        )
    }

    when (val pState = profileState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchBlack),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PitchBlack)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyArenaState(
                    title = "Profile Load Error",
                    subtitle = pState.message
                )
            }
        }

        is UiState.Success -> {
            val profile = pState.data
            if (profile == null) {
                CompleteProfileScreen(
                    initialName = initialDisplayName,
                    isBusy = isBusy,
                    onSubmitProfile = viewModel::createPlayerProfile,
                    onSignOut = doSignOut
                )
            } else {
                var activeDestination by remember { mutableStateOf(AppDestination.HOME) }
                var modeFilter by remember { mutableStateOf<TournamentMode?>(null) }
                var detailTournamentId by remember { mutableStateOf<String?>(null) }
                var showWalletScreen by remember { mutableStateOf(false) }
                var walletInitialAction by remember { mutableStateOf<WalletQuickAction?>(null) }

                val openWallet: (WalletQuickAction?) -> Unit = { action ->
                    walletInitialAction = action
                    showWalletScreen = true
                }

                val destinations = remember(isAdmin) {
                    if (isAdmin) AppDestination.entries
                    else AppDestination.entries.filter { it != AppDestination.ADMIN }
                }

                val joinedIds = remember(myRegistrationsState) {
                    (myRegistrationsState as? UiState.Success)?.data
                        ?.map { it.tournamentId }
                        ?.toSet()
                        .orEmpty()
                }

                val detailTournament = remember(tournamentsState, detailTournamentId) {
                    val tid = detailTournamentId ?: return@remember null
                    (tournamentsState as? UiState.Success)?.data?.firstOrNull { it.id == tid }
                }

                LaunchedEffect(detailTournamentId) {
                    if (detailTournamentId != null) {
                        viewModel.selectTournament(detailTournamentId)
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PitchBlack)
                ) {
                    val useNavRail = maxWidth >= 700.dp
                    val showChrome = detailTournament == null && !showWalletScreen

                    Scaffold(
                        containerColor = PitchBlack,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.navigationBarsPadding()
                            )
                        },
                        bottomBar = {
                            if (!useNavRail && showChrome) {
                                NavigationBar(
                                    containerColor = CyberSurface,
                                    tonalElevation = 0.dp
                                ) {
                                    destinations.forEach { dest ->
                                        val selected = activeDestination == dest
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { activeDestination = dest },
                                            icon = {
                                                Icon(
                                                    imageVector = dest.icon,
                                                    contentDescription = dest.label
                                                )
                                            },
                                            label = { Text(dest.label, maxLines = 1) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = if (dest == AppDestination.ADMIN) GoldTrophy else NeonGreen,
                                                selectedTextColor = if (dest == AppDestination.ADMIN) GoldTrophy else NeonGreen,
                                                indicatorColor = DarkEmeraldContainer,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .statusBarsPadding()
                        ) {
                            if (useNavRail && showChrome) {
                                NavigationRail(
                                    containerColor = CyberCard,
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .navigationBarsPadding()
                                ) {
                                    destinations.forEach { dest ->
                                        val selected = activeDestination == dest
                                        NavigationRailItem(
                                            selected = selected,
                                            onClick = { activeDestination = dest },
                                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                                            label = { Text(dest.label) },
                                            colors = NavigationRailItemDefaults.colors(
                                                selectedIconColor = if (dest == AppDestination.ADMIN) GoldTrophy else NeonGreen,
                                                selectedTextColor = if (dest == AppDestination.ADMIN) GoldTrophy else NeonGreen,
                                                indicatorColor = DarkEmeraldContainer,
                                                unselectedIconColor = TextSecondary,
                                                unselectedTextColor = TextSecondary
                                            ),
                                            modifier = Modifier.testTag("rail_${dest.name.lowercase()}")
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (showChrome) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(CyberSurface)
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_gamerzone_icon),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(1.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.brand_title),
                                                fontFamily = RussoOneFontFamily,
                                                fontSize = 15.sp,
                                                color = TextPrimary
                                            )
                                        }

                                        Surface(
                                            onClick = { openWallet(null) },
                                            shape = RoundedCornerShape(20.dp),
                                            color = DarkEmeraldContainer,
                                            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f)),
                                            modifier = Modifier
                                                .height(36.dp)
                                                .testTag("header_wallet_pill")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                                    contentDescription = "Open My Wallet",
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${profile.walletBalancePkr} PKR",
                                                    fontFamily = BebasNeueFontFamily,
                                                    fontSize = 18.sp,
                                                    color = NeonGreen
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.Add,
                                                    contentDescription = null,
                                                    tint = NeonGreen,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    when {
                                        showWalletScreen -> {
                                            WalletScreen(
                                                profile = profile,
                                                transactionsState = myWalletTransactionsState,
                                                depositRequestsState = myDepositRequestsState,
                                                withdrawalRequestsState = myWithdrawalRequestsState,
                                                initialAction = walletInitialAction,
                                                isBusy = isBusy,
                                                onConsumeInitialAction = { walletInitialAction = null },
                                                onBack = { showWalletScreen = false },
                                                onSubmitDeposit = { amt, method, tid, sender, b64 ->
                                                    viewModel.submitDepositRequest(profile, amt, method, tid, sender, b64)
                                                },
                                                onSubmitWithdrawal = { amt, method, accNum, accTitle ->
                                                    viewModel.submitWithdrawalRequest(profile, amt, method, accNum, accTitle)
                                                }
                                            )
                                        }

                                        detailTournament != null -> {
                                            TournamentDetailScreen(
                                                tournament = detailTournament,
                                                profile = profile,
                                                isAdmin = isAdmin,
                                                registrationsState = selectedRegistrationsState,
                                                isBusy = isBusy,
                                                onBack = { detailTournamentId = null },
                                                onJoinTournament = viewModel::joinTournament,
                                                onOpenAddMoney = { openWallet(WalletQuickAction.ADD_MONEY) },
                                                modifier = Modifier.navigationBarsPadding()
                                            )
                                        }

                                        else -> {
                                            when (activeDestination) {
                                                AppDestination.HOME -> HomeScreen(
                                                    profile = profile,
                                                    tournamentsState = tournamentsState,
                                                    leaderboardState = leaderboardState,
                                                    announcementsState = announcementsState,
                                                    joinedTournamentIds = joinedIds,
                                                    onOpenModeFilter = { mode ->
                                                        modeFilter = mode
                                                        activeDestination = AppDestination.TOURNAMENTS
                                                    },
                                                    onOpenTournaments = {
                                                        modeFilter = null
                                                        activeDestination = AppDestination.TOURNAMENTS
                                                    },
                                                    onSelectTournament = { tid -> detailTournamentId = tid },
                                                    onOpenWallet = openWallet
                                                )

                                                AppDestination.TOURNAMENTS -> TournamentsScreen(
                                                    tournamentsState = tournamentsState,
                                                    joinedTournamentIds = joinedIds,
                                                    initialModeFilter = modeFilter,
                                                    onClearInitialMode = { modeFilter = null },
                                                    onSelectTournament = { tid -> detailTournamentId = tid }
                                                )

                                                AppDestination.LEADERBOARD -> LeaderboardScreen(
                                                    leaderboardState = leaderboardState,
                                                    teamRankings = teamRankings,
                                                    currentUserId = currentUserId
                                                )

                                                AppDestination.PLAYER -> PlayerDashboardScreen(
                                                    profile = profile,
                                                    isAdmin = isAdmin,
                                                    myRegistrationsState = myRegistrationsState,
                                                    announcementsState = announcementsState,
                                                    isBusy = isBusy,
                                                    onOpenTournament = { tid -> detailTournamentId = tid },
                                                    onOpenWallet = openWallet,
                                                    onUpdateProfile = viewModel::updatePlayerProfile,
                                                    onSignOut = doSignOut
                                                )

                                                AppDestination.ADMIN -> AdminDashboardScreen(
                                                    tournamentsState = tournamentsState,
                                                    leaderboardState = leaderboardState,
                                                    announcementsState = announcementsState,
                                                    depositRequestsState = allDepositRequestsState,
                                                    withdrawalRequestsState = allWithdrawalRequestsState,
                                                    selectedAdminTournamentId = selectedTournamentId,
                                                    selectedRegistrationsState = selectedRegistrationsState,
                                                    isBusy = isBusy,
                                                    onSelectAdminTournament = viewModel::selectTournament,
                                                    onCreateTournament = viewModel::createTournament,
                                                    onUpdateTournament = viewModel::updateTournament,
                                                    onDeleteTournament = viewModel::deleteTournament,
                                                    onPublishResult = viewModel::publishPlayerResult,
                                                    onAdjustPlayerStats = viewModel::adjustPlayerStats,
                                                    onCreateAnnouncement = viewModel::createAnnouncement,
                                                    onDeleteAnnouncement = viewModel::deleteAnnouncement,
                                                    onSeedStarters = viewModel::seedStarterTournaments,
                                                    onReviewDeposit = viewModel::reviewDepositRequest,
                                                    onReviewWithdrawal = viewModel::reviewWithdrawalRequest
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
        }
    }
}
