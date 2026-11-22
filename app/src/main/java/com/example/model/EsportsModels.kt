package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TournamentMode(val label: String) {
    SOLO("Solo"),
    DUO("Duo"),
    SQUAD("Squad"),
    CLASH_SQUAD("Clash Squad"),
    BATTLE_ROYALE("Battle Royale");

    companion object {
        fun fromRaw(raw: String): TournamentMode =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) }
                ?: SQUAD
    }
}

enum class TournamentStatus(val raw: String, val label: String) {
    UPCOMING("upcoming", "Upcoming"),
    LIVE("live", "Live"),
    COMPLETED("completed", "Completed");

    companion object {
        fun fromRaw(raw: String): TournamentStatus =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: UPCOMING
    }
}

enum class AnnouncementCategory(val raw: String, val label: String) {
    TOURNAMENT("tournament", "Tournament"),
    WINNER("winner", "Winner"),
    ALERT("alert", "Alert"),
    RULES("rules", "Rules");

    companion object {
        fun fromRaw(raw: String): AnnouncementCategory =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: TOURNAMENT
    }
}

enum class PaymentMethod(
    val raw: String,
    val displayName: String,
    val accountNumber: String
) {
    EASYPAISA("easypaisa", "EasyPaisa", "03489674834"),
    JAZZCASH("jazzcash", "JazzCash", "03406690336");

    companion object {
        fun fromRaw(raw: String): PaymentMethod =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: EASYPAISA
    }
}

enum class RequestStatus(val raw: String, val label: String) {
    PENDING("pending", "Pending"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun fromRaw(raw: String): RequestStatus =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: PENDING
    }
}

enum class WalletTxType(val raw: String, val label: String) {
    DEPOSIT("deposit", "Deposit"),
    TOURNAMENT_ENTRY("tournament_entry", "Tournament Fee"),
    WITHDRAWAL("withdrawal", "Withdrawal"),
    WITHDRAWAL_REFUND("withdrawal_refund", "Refund"),
    PRIZE_PAYOUT("prize_payout", "Prize Win");

    companion object {
        fun fromRaw(raw: String): WalletTxType =
            entries.firstOrNull { it.raw.equals(raw, ignoreCase = true) } ?: DEPOSIT
    }
}

@Keep
@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val inGameName: String = "",
    val freeFireUid: String = "",
    val whatsappNumber: String = "",
    val teamName: String = "",
    val role: String = "player",
    val matchesPlayed: Long = 0L,
    val totalWins: Long = 0L,
    val matchPoints: Long = 0L,
    val killPoints: Long = 0L,
    val totalPoints: Long = 0L,
    val walletBalancePkr: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val inGameName: String = "",
    val freeFireUid: String = "",
    val teamName: String = "",
    val matchesPlayed: Long = 0L,
    val totalWins: Long = 0L,
    val matchPoints: Long = 0L,
    val killPoints: Long = 0L,
    val totalPoints: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class TeamRanking(
    val teamName: String,
    val memberCount: Int,
    val totalWins: Long,
    val matchPoints: Long,
    val killPoints: Long,
    val totalPoints: Long
)

@Keep
@IgnoreExtraProperties
data class Tournament(
    @DocumentId val id: String = "",
    val title: String = "",
    val mode: String = TournamentMode.SQUAD.label,
    val status: String = TournamentStatus.UPCOMING.raw,
    val mapName: String = "Bermuda",
    val entryFeePkr: Long = 0L,
    val prizePoolPkr: Long = 0L,
    val firstPrizePkr: Long = 0L,
    val secondPrizePkr: Long = 0L,
    val thirdPrizePkr: Long = 0L,
    val matchTime: String = "",
    val totalSlots: Long = 48L,
    val registeredCount: Long = 0L,
    val rules: String = "",
    val roomId: String = "",
    val roomPassword: String = "",
    val winnerName: String = "",
    val winnerUid: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TournamentRegistration(
    @DocumentId val id: String = "",
    val tournamentId: String = "",
    val tournamentTitle: String = "",
    val tournamentMode: String = TournamentMode.SQUAD.label,
    val tournamentStatus: String = TournamentStatus.UPCOMING.raw,
    val matchTime: String = "",
    val mapName: String = "",
    val entryFeePkr: Long = 0L,
    val prizePoolPkr: Long = 0L,
    val userId: String = "",
    val inGameName: String = "",
    val freeFireUid: String = "",
    val teamName: String = "",
    val slotNumber: Long = 1L,
    val matchPoints: Long = 0L,
    val killPoints: Long = 0L,
    val totalPoints: Long = 0L,
    val placement: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = AnnouncementCategory.TOURNAMENT.raw,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val inGameName: String = "",
    val userEmail: String = "",
    val amountPkr: Long = 0L,
    val paymentMethod: String = PaymentMethod.EASYPAISA.raw,
    val receiverAccountNumber: String = PaymentMethod.EASYPAISA.accountNumber,
    val transactionId: String = "",
    val senderNumber: String = "",
    val screenshotBase64: String = "",
    val status: String = RequestStatus.PENDING.raw,
    val adminNote: String = "",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val inGameName: String = "",
    val userEmail: String = "",
    val amountPkr: Long = 0L,
    val paymentMethod: String = PaymentMethod.EASYPAISA.raw,
    val accountNumber: String = "",
    val accountTitle: String = "",
    val status: String = RequestStatus.PENDING.raw,
    val adminNote: String = "",
    val reviewedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = WalletTxType.DEPOSIT.raw,
    val direction: String = "credit",
    val amountPkr: Long = 0L,
    val title: String = "",
    val subtitle: String = "",
    val referenceId: String = "",
    val status: String = "completed",
    val balanceAfterPkr: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
