package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GamerZoneRepository
import com.example.model.AdminWalletTransaction
import com.example.model.Announcement
import com.example.model.AnnouncementCategory
import com.example.model.DepositRequest
import com.example.model.LeaderboardEntry
import com.example.model.PaymentMethod
import com.example.model.PlayerProfile
import com.example.model.TeamRanking
import com.example.model.Tournament
import com.example.model.TournamentMode
import com.example.model.TournamentRegistration
import com.example.model.TournamentStatus
import com.example.model.UiState
import com.example.model.WalletTransaction
import com.example.model.WithdrawalRequest
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class GamerZoneViewModel(
    private val repository: GamerZoneRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedTournamentId = MutableStateFlow<String?>(null)
    val selectedTournamentId: StateFlow<String?> = _selectedTournamentId.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    val profileState: StateFlow<UiState<PlayerProfile?>> =
        repository.observeMyProfile(currentUserId)
            .toUiStateFlow("observeProfile")

    val isAdmin: StateFlow<Boolean> =
        repository.observeIsAdmin(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                emit(false)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val tournamentsState: StateFlow<UiState<List<Tournament>>> =
        repository.observeTournaments()
            .toUiStateFlow("observeTournaments")

    val myRegistrationsState: StateFlow<UiState<List<TournamentRegistration>>> =
        repository.observeMyRegistrations(currentUserId)
            .toUiStateFlow("observeMyRegistrations")

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeLeaderboard()
            .toUiStateFlow("observeLeaderboard")

    val announcementsState: StateFlow<UiState<List<Announcement>>> =
        repository.observeAnnouncements()
            .toUiStateFlow("observeAnnouncements")

    val myWalletTransactionsState: StateFlow<UiState<List<WalletTransaction>>> =
        repository.observeMyWalletTransactions(currentUserId)
            .toUiStateFlow("observeMyWalletTransactions")

    val myDepositRequestsState: StateFlow<UiState<List<DepositRequest>>> =
        repository.observeMyDepositRequests(currentUserId)
            .toUiStateFlow("observeMyDepositRequests")

    val myWithdrawalRequestsState: StateFlow<UiState<List<WithdrawalRequest>>> =
        repository.observeMyWithdrawalRequests(currentUserId)
            .toUiStateFlow("observeMyWithdrawalRequests")

    val allDepositRequestsState: StateFlow<UiState<List<DepositRequest>>> =
        isAdmin
            .flatMapLatest { admin ->
                if (admin) repository.observeAllDepositRequests() else flowOf(emptyList())
            }
            .toUiStateFlow("observeAllDepositRequests")

    val allWithdrawalRequestsState: StateFlow<UiState<List<WithdrawalRequest>>> =
        isAdmin
            .flatMapLatest { admin ->
                if (admin) repository.observeAllWithdrawalRequests() else flowOf(emptyList())
            }
            .toUiStateFlow("observeAllWithdrawalRequests")

    val allAdminTransactionsState: StateFlow<UiState<List<AdminWalletTransaction>>> =
        isAdmin
            .flatMapLatest { admin ->
                if (admin) repository.observeAllAdminTransactions() else flowOf(emptyList())
            }
            .toUiStateFlow("observeAllAdminTransactions")

    val selectedTournamentRegistrations: StateFlow<UiState<List<TournamentRegistration>>> =
        _selectedTournamentId
            .flatMapLatest { tid ->
                if (tid.isNullOrBlank()) flowOf(emptyList())
                else repository.observeTournamentRegistrations(tid)
            }
            .toUiStateFlow("observeTournamentRegistrations")

    val teamRankings: StateFlow<List<TeamRanking>> =
        leaderboardState
            .map { state ->
                val entries = (state as? UiState.Success)?.data ?: emptyList()
                entries
                    .groupBy { it.teamName.ifBlank { "Solo Squad" } }
                    .map { (team, members) ->
                        TeamRanking(
                            teamName = team,
                            memberCount = members.size,
                            totalWins = members.sumOf { it.totalWins },
                            matchPoints = members.sumOf { it.matchPoints },
                            killPoints = members.sumOf { it.killPoints },
                            totalPoints = members.sumOf { it.totalPoints }
                        )
                    }
                    .sortedByDescending { it.totalPoints }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun selectTournament(tournamentId: String?) {
        _selectedTournamentId.value = tournamentId
    }

    fun createPlayerProfile(
        inGameName: String,
        freeFireUid: String,
        teamName: String,
        whatsappNumber: String
    ) = runAction("Profile registered! Welcome to GAMER ZONE PK.") {
        repository.createPlayerProfile(inGameName, freeFireUid, teamName, whatsappNumber)
    }

    fun updatePlayerProfile(
        displayName: String,
        inGameName: String,
        freeFireUid: String,
        teamName: String,
        whatsappNumber: String
    ) = runAction("Profile updated successfully.") {
        repository.updatePlayerProfile(displayName, inGameName, freeFireUid, teamName, whatsappNumber)
    }

    fun joinTournament(tournament: Tournament, profile: PlayerProfile) =
        runAction(
            if (tournament.entryFeePkr > 0L) "Registered! ${tournament.entryFeePkr} PKR deducted from Wallet."
            else "Registered for ${tournament.title}!"
        ) {
            repository.joinTournament(tournament, profile)
        }

    fun submitDepositRequest(
        profile: PlayerProfile,
        amountPkr: Long,
        paymentMethod: PaymentMethod,
        transactionId: String,
        senderNumber: String,
        screenshotBase64: String
    ) = runAction("Deposit request submitted for admin verification!") {
        repository.submitDepositRequest(profile, amountPkr, paymentMethod, transactionId, senderNumber, screenshotBase64)
    }

    fun submitWithdrawalRequest(
        profile: PlayerProfile,
        amountPkr: Long,
        paymentMethod: PaymentMethod,
        accountNumber: String,
        accountTitle: String
    ) = runAction("Withdrawal request submitted! Amount held until payout.") {
        repository.submitWithdrawalRequest(profile, amountPkr, paymentMethod, accountNumber, accountTitle)
    }

    fun reviewDepositRequest(request: DepositRequest, approve: Boolean, adminNote: String) =
        runAction(if (approve) "Deposit approved & PKR credited!" else "Deposit request rejected.") {
            repository.reviewDepositRequest(request, approve, adminNote)
        }

    fun reviewWithdrawalRequest(request: WithdrawalRequest, approve: Boolean, adminNote: String) =
        runAction(if (approve) "Withdrawal marked as paid!" else "Withdrawal rejected & balance refunded.") {
            repository.reviewWithdrawalRequest(request, approve, adminNote)
        }

    fun createTournament(
        title: String,
        mode: TournamentMode,
        status: TournamentStatus,
        mapName: String,
        entryFeePkr: Long,
        prizePoolPkr: Long,
        firstPrizePkr: Long,
        secondPrizePkr: Long,
        thirdPrizePkr: Long,
        matchTime: String,
        totalSlots: Long,
        rules: String,
        roomId: String,
        roomPassword: String
    ) = runAction("Tournament published!") {
        repository.createTournament(
            title, mode, status, mapName, entryFeePkr, prizePoolPkr,
            firstPrizePkr, secondPrizePkr, thirdPrizePkr, matchTime, totalSlots,
            rules, roomId, roomPassword
        )
    }

    fun updateTournament(
        tournamentId: String,
        title: String,
        mode: TournamentMode,
        status: TournamentStatus,
        mapName: String,
        entryFeePkr: Long,
        prizePoolPkr: Long,
        firstPrizePkr: Long,
        secondPrizePkr: Long,
        thirdPrizePkr: Long,
        matchTime: String,
        totalSlots: Long,
        rules: String,
        roomId: String,
        roomPassword: String
    ) = runAction("Tournament updated!") {
        repository.updateTournament(
            tournamentId, title, mode, status, mapName, entryFeePkr, prizePoolPkr,
            firstPrizePkr, secondPrizePkr, thirdPrizePkr, matchTime, totalSlots,
            rules, roomId, roomPassword
        )
    }

    fun deleteTournament(tournamentId: String) =
        runAction("Tournament deleted.") {
            repository.deleteTournament(tournamentId)
        }

    fun publishPlayerResult(
        tournament: Tournament,
        registration: TournamentRegistration,
        matchPoints: Long,
        killPoints: Long,
        placement: Long,
        markAsWinner: Boolean,
        prizePayoutPkr: Long = 0L
    ) = runAction(
        when {
            prizePayoutPkr > 0L -> "Winner published & $prizePayoutPkr PKR credited!"
            markAsWinner -> "Winner published & points awarded!"
            else -> "Result updated!"
        }
    ) {
        repository.publishPlayerResult(
            tournament, registration, matchPoints, killPoints, placement, markAsWinner, prizePayoutPkr
        )
    }

    fun adjustPlayerStats(
        entry: LeaderboardEntry,
        matchesDelta: Long,
        winsDelta: Long,
        matchPointsDelta: Long,
        killPointsDelta: Long
    ) = runAction("Player stats updated.") {
        repository.adjustLeaderboardPlayerStats(entry, matchesDelta, winsDelta, matchPointsDelta, killPointsDelta)
    }

    fun createAnnouncement(title: String, message: String, category: AnnouncementCategory) =
        runAction("Announcement broadcasted!") {
            repository.createAnnouncement(title, message, category)
        }

    fun deleteAnnouncement(announcementId: String) =
        runAction("Announcement removed.") {
            repository.deleteAnnouncement(announcementId)
        }

    fun seedStarterTournaments() =
        runAction("Starter Free Fire tournaments published!") {
            repository.seedStarterTournaments()
        }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result.onSuccess {
                _messages.tryEmit(successMessage)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Operation failed")
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(tag: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error in $tag", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load data"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    private companion object {
        const val TAG = "GamerZoneVM"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
