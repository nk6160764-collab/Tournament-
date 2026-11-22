package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdminWalletTransaction
import com.example.model.Announcement
import com.example.model.AnnouncementCategory
import com.example.model.DepositRequest
import com.example.model.LeaderboardEntry
import com.example.model.PaymentMethod
import com.example.model.PlayerProfile
import com.example.model.RequestStatus
import com.example.model.Tournament
import com.example.model.TournamentMode
import com.example.model.TournamentRegistration
import com.example.model.TournamentStatus
import com.example.model.WalletLimits
import com.example.model.WalletTransaction
import com.example.model.WalletTxType
import com.example.model.WithdrawalRequest
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class GamerZoneRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun isBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email == "nk6160764@gmail.com" && user.isEmailVerified
    }

    // --- REALTIME STREAMS ---

    fun observeMyProfile(userId: String): Flow<PlayerProfile?> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(PlayerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, path, userId)
                throw error
            }
    }

    fun observeIsAdmin(userId: String): Flow<Boolean> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        if (isBootstrappedAdmin()) {
            return flow { emit(true) }
        }
        val path = "admins/$userId"
        return db.collection("admins").document(userId)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                emit(false)
            }
    }

    fun observeTournaments(): Flow<List<Tournament>> {
        val uid = requireUserId()
        val path = "tournaments"
        return db.collection("tournaments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Tournament::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeTournamentRegistrations(tournamentId: String): Flow<List<TournamentRegistration>> {
        val uid = requireUserId()
        require(tournamentId.isNotBlank()) { "Tournament ID must not be blank" }
        val path = "tournaments/$tournamentId/registrations"
        return db.collection("tournaments").document(tournamentId)
            .collection("registrations")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TournamentRegistration::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.slotNumber }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeMyRegistrations(userId: String): Flow<List<TournamentRegistration>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/registrations"
        return db.collection("users").document(userId)
            .collection("registrations")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TournamentRegistration::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> {
        val uid = requireUserId()
        val path = "leaderboard"
        return db.collection("leaderboard")
            .orderBy("totalPoints", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeAnnouncements(): Flow<List<Announcement>> {
        val uid = requireUserId()
        val path = "announcements"
        return db.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeMyWalletTransactions(userId: String): Flow<List<WalletTransaction>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "users/$userId/walletTransactions"
        return db.collection("users").document(userId)
            .collection("walletTransactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WalletTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeMyDepositRequests(userId: String): Flow<List<DepositRequest>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "depositRequests"
        return db.collection("depositRequests")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DepositRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeMyWithdrawalRequests(userId: String): Flow<List<WithdrawalRequest>> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        val path = "withdrawalRequests"
        return db.collection("withdrawalRequests")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WithdrawalRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, userId)
                throw error
            }
    }

    fun observeAllDepositRequests(): Flow<List<DepositRequest>> {
        val uid = requireUserId()
        val path = "depositRequests"
        return db.collection("depositRequests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DepositRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeAllWithdrawalRequests(): Flow<List<WithdrawalRequest>> {
        val uid = requireUserId()
        val path = "withdrawalRequests"
        return db.collection("withdrawalRequests")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    WithdrawalRequest::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeAllAdminTransactions(): Flow<List<AdminWalletTransaction>> {
        val uid = requireUserId()
        val path = "adminTransactions"
        return db.collection("adminTransactions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AdminWalletTransaction::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }

    // --- PLAYER PROFILE MUTATIONS ---

    suspend fun createPlayerProfile(
        inGameName: String,
        freeFireUid: String,
        teamName: String,
        whatsappNumber: String
    ): Result<Unit> {
        val currentUser = auth.currentUser
        var path = "users/${currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanIgn = inGameName.trim()
            val cleanFfUid = freeFireUid.trim()
            val cleanTeam = teamName.trim().ifEmpty { "Solo Wolf" }
            val cleanWa = whatsappNumber.trim()
            val displayName = (currentUser?.displayName?.trim()?.take(60)).orEmpty().ifEmpty { cleanIgn }
            val email = (currentUser?.email?.trim()?.take(200)).orEmpty().ifEmpty { "$uid@player.gamerzonepk" }

            require(cleanIgn.length in 2..40) { "In-Game Name must be 2-40 characters." }
            require(cleanFfUid.length in 4..20) { "Free Fire UID must be 4-20 characters." }
            require(cleanTeam.length in 1..40) { "Team Name must be 1-40 characters." }
            require(cleanWa.length <= 25) { "WhatsApp number must be at most 25 characters." }

            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)
            path = userRef.path

            val userPayload = mapOf(
                "uid" to uid,
                "displayName" to displayName,
                "email" to email,
                "inGameName" to cleanIgn,
                "freeFireUid" to cleanFfUid,
                "whatsappNumber" to cleanWa,
                "teamName" to cleanTeam,
                "role" to "player",
                "matchesPlayed" to 0L,
                "totalWins" to 0L,
                "matchPoints" to 0L,
                "killPoints" to 0L,
                "totalPoints" to 0L,
                "walletBalancePkr" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val lbPayload = mapOf(
                "userId" to uid,
                "inGameName" to cleanIgn,
                "freeFireUid" to cleanFfUid,
                "teamName" to cleanTeam,
                "matchesPlayed" to 0L,
                "totalWins" to 0L,
                "matchPoints" to 0L,
                "killPoints" to 0L,
                "totalPoints" to 0L,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(userRef, userPayload)
            batch.set(lbRef, lbPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePlayerProfile(
        displayName: String,
        inGameName: String,
        freeFireUid: String,
        teamName: String,
        whatsappNumber: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanDisplay = displayName.trim()
            val cleanIgn = inGameName.trim()
            val cleanFfUid = freeFireUid.trim()
            val cleanTeam = teamName.trim()
            val cleanWa = whatsappNumber.trim()

            require(cleanDisplay.length in 1..60) { "Display name must be 1-60 characters." }
            require(cleanIgn.length in 2..40) { "In-Game Name must be 2-40 characters." }
            require(cleanFfUid.length in 4..20) { "Free Fire UID must be 4-20 characters." }
            require(cleanTeam.length in 1..40) { "Team Name must be 1-40 characters." }
            require(cleanWa.length <= 25) { "WhatsApp number must be at most 25 characters." }

            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard").document(uid)
            path = userRef.path

            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "displayName" to cleanDisplay,
                    "inGameName" to cleanIgn,
                    "freeFireUid" to cleanFfUid,
                    "teamName" to cleanTeam,
                    "whatsappNumber" to cleanWa,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                lbRef,
                mapOf(
                    "inGameName" to cleanIgn,
                    "freeFireUid" to cleanFfUid,
                    "teamName" to cleanTeam,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- PLAYER TOURNAMENT JOIN (ATOMIC WALLET FEE DEDUCTION) ---

    suspend fun joinTournament(tournament: Tournament, profile: PlayerProfile): Result<Unit> {
        var path = "tournaments/${tournament.id}/registrations"
        return try {
            val uid = requireUserId()
            require(tournament.id.isNotBlank()) { "Invalid tournament ID" }

            val tournamentRef = db.collection("tournaments").document(tournament.id)
            val tourneyRegRef = tournamentRef.collection("registrations").document(uid)
            val userRef = db.collection("users").document(uid)
            val userRegRef = userRef.collection("registrations").document(tournament.id)
            val txDocRef = userRef.collection("walletTransactions").document()
            path = tourneyRegRef.path

            db.runTransaction { tx ->
                val tourneySnap = tx.get(tournamentRef)
                if (!tourneySnap.exists()) {
                    throw IllegalStateException("Tournament no longer exists.")
                }
                val regSnap = tx.get(tourneyRegRef)
                if (regSnap.exists()) {
                    throw IllegalStateException("You are already registered in this tournament.")
                }
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("Complete your player profile first.")
                }

                val liveStatus = tourneySnap.getString("status") ?: tournament.status
                if (liveStatus != TournamentStatus.UPCOMING.raw) {
                    throw IllegalStateException("Registrations are open for upcoming matches only.")
                }
                val liveTotalSlots = tourneySnap.getLong("totalSlots") ?: tournament.totalSlots
                val liveRegistered = tourneySnap.getLong("registeredCount") ?: 0L
                if (liveRegistered >= liveTotalSlots) {
                    throw IllegalStateException("All tournament slots are full.")
                }

                val liveEntryFee = (tourneySnap.getLong("entryFeePkr") ?: tournament.entryFeePkr).coerceAtLeast(0L)
                val currentBalance = (userSnap.getLong("walletBalancePkr") ?: 0L).coerceAtLeast(0L)
                if (currentBalance < liveEntryFee) {
                    throw IllegalStateException(
                        "Insufficient wallet balance ($currentBalance PKR). Entry fee is $liveEntryFee PKR."
                    )
                }

                val liveTitle = tourneySnap.getString("title") ?: tournament.title
                val liveMode = tourneySnap.getString("mode") ?: tournament.mode
                val liveMatchTime = tourneySnap.getString("matchTime") ?: tournament.matchTime
                val liveMap = tourneySnap.getString("mapName") ?: tournament.mapName
                val livePrizePool = tourneySnap.getLong("prizePoolPkr") ?: tournament.prizePoolPkr
                val slotNum = (liveRegistered + 1L).coerceAtMost(liveTotalSlots)

                val regPayload = mapOf(
                    "tournamentId" to tournament.id,
                    "tournamentTitle" to liveTitle,
                    "tournamentMode" to liveMode,
                    "tournamentStatus" to liveStatus,
                    "matchTime" to liveMatchTime,
                    "mapName" to liveMap,
                    "entryFeePkr" to liveEntryFee,
                    "prizePoolPkr" to livePrizePool,
                    "userId" to uid,
                    "inGameName" to profile.inGameName,
                    "freeFireUid" to profile.freeFireUid,
                    "teamName" to profile.teamName,
                    "slotNumber" to slotNum,
                    "matchPoints" to 0L,
                    "killPoints" to 0L,
                    "totalPoints" to 0L,
                    "placement" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                tx.set(tourneyRegRef, regPayload)
                tx.set(userRegRef, regPayload)
                tx.update(
                    tournamentRef,
                    mapOf(
                        "registeredCount" to (liveRegistered + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (liveEntryFee > 0L) {
                    val newBalance = currentBalance - liveEntryFee
                    val txTitle = "Joined: $liveTitle".take(100)
                    val txSubtitle = "$liveMode • $liveMap • Slot #$slotNum".take(150)
                    val adminTxRef = db.collection("adminTransactions").document(txDocRef.id)
                    tx.update(
                        userRef,
                        mapOf(
                            "walletBalancePkr" to newBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        txDocRef,
                        mapOf(
                            "userId" to uid,
                            "type" to WalletTxType.TOURNAMENT_ENTRY.raw,
                            "direction" to "debit",
                            "amountPkr" to liveEntryFee,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to tournament.id,
                            "status" to "completed",
                            "balanceAfterPkr" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        adminTxRef,
                        mapOf(
                            "userId" to uid,
                            "inGameName" to profile.inGameName,
                            "userEmail" to profile.email,
                            "type" to WalletTxType.TOURNAMENT_ENTRY.raw,
                            "direction" to "debit",
                            "amountPkr" to liveEntryFee,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to tournament.id,
                            "status" to "completed",
                            "balanceAfterPkr" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PLAYER WALLET DEPOSIT & WITHDRAWAL ---

    private fun buildTidKey(paymentMethod: PaymentMethod, transactionId: String): String {
        val sanitized = transactionId.trim().uppercase().replace(Regex("[^A-Z0-9_-]"), "_")
        return "${paymentMethod.raw}_$sanitized".take(128)
    }

    suspend fun submitDepositRequest(
        profile: PlayerProfile,
        amountPkr: Long,
        paymentMethod: PaymentMethod,
        transactionId: String,
        senderNumber: String,
        screenshotBase64: String
    ): Result<Unit> {
        var path = "depositRequests"
        return try {
            val uid = requireUserId()
            val cleanTid = transactionId.trim()
            val cleanSender = senderNumber.trim()
            val cleanScreenshot = screenshotBase64.trim()
            require(amountPkr in WalletLimits.MIN_DEPOSIT_PKR..WalletLimits.MAX_DEPOSIT_PKR) {
                "Minimum deposit amount is ${WalletLimits.MIN_DEPOSIT_PKR} PKR."
            }
            require(cleanTid.length in 4..64) { "Enter a valid Transaction ID (4-64 characters)." }
            require(cleanSender.length <= 25) { "Sender number must be at most 25 characters." }
            require(cleanScreenshot.length in 20..600_000) { "Please attach a valid payment screenshot." }

            val tidKey = buildTidKey(paymentMethod, cleanTid)
            val reqRef = db.collection("depositRequests").document()
            val tidLockRef = db.collection("usedTransactionIds").document(tidKey)
            path = reqRef.path

            db.runTransaction { tx ->
                val existingLock = tx.get(tidLockRef)
                if (existingLock.exists()) {
                    throw IllegalStateException(
                        "This Transaction ID ($cleanTid) has already been submitted. Duplicate transactions are not allowed."
                    )
                }

                tx.set(
                    reqRef,
                    mapOf(
                        "userId" to uid,
                        "inGameName" to profile.inGameName,
                        "userEmail" to profile.email,
                        "amountPkr" to amountPkr,
                        "paymentMethod" to paymentMethod.raw,
                        "receiverAccountNumber" to paymentMethod.accountNumber,
                        "transactionId" to cleanTid,
                        "tidKey" to tidKey,
                        "senderNumber" to cleanSender,
                        "screenshotBase64" to cleanScreenshot,
                        "status" to RequestStatus.PENDING.raw,
                        "adminNote" to "",
                        "reviewedBy" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    tidLockRef,
                    mapOf(
                        "userId" to uid,
                        "depositId" to reqRef.id,
                        "transactionId" to cleanTid,
                        "paymentMethod" to paymentMethod.raw,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        profile: PlayerProfile,
        amountPkr: Long,
        paymentMethod: PaymentMethod,
        accountNumber: String,
        accountTitle: String
    ): Result<Unit> {
        var path = "withdrawalRequests"
        return try {
            val uid = requireUserId()
            val cleanAcc = accountNumber.trim()
            val cleanTitle = accountTitle.trim()
            require(amountPkr in WalletLimits.MIN_WITHDRAWAL_PKR..WalletLimits.MAX_WITHDRAWAL_PKR) {
                "Minimum withdrawal amount is ${WalletLimits.MIN_WITHDRAWAL_PKR} PKR."
            }
            require(cleanAcc.length in 7..20) { "Enter a valid ${paymentMethod.displayName} account number." }
            require(cleanTitle.length in 2..60) { "Enter the account holder name (2-60 characters)." }

            val userRef = db.collection("users").document(uid)
            val wdRef = db.collection("withdrawalRequests").document()
            val txRef = userRef.collection("walletTransactions").document()
            val adminTxRef = db.collection("adminTransactions").document(txRef.id)
            path = wdRef.path

            db.runTransaction { tx ->
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("Player profile not found.")
                val currentBalance = (userSnap.getLong("walletBalancePkr") ?: 0L).coerceAtLeast(0L)
                if (currentBalance < amountPkr) {
                    throw IllegalStateException("Insufficient balance ($currentBalance PKR available).")
                }
                val newBalance = currentBalance - amountPkr
                val txTitle = "${paymentMethod.displayName} Withdrawal"
                val txSubtitle = "To: $cleanAcc ($cleanTitle)".take(150)

                tx.set(
                    wdRef,
                    mapOf(
                        "userId" to uid,
                        "inGameName" to profile.inGameName,
                        "userEmail" to profile.email,
                        "amountPkr" to amountPkr,
                        "paymentMethod" to paymentMethod.raw,
                        "accountNumber" to cleanAcc,
                        "accountTitle" to cleanTitle,
                        "status" to RequestStatus.PENDING.raw,
                        "adminNote" to "",
                        "reviewedBy" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.update(
                    userRef,
                    mapOf(
                        "walletBalancePkr" to newBalance,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "type" to WalletTxType.WITHDRAWAL.raw,
                        "direction" to "debit",
                        "amountPkr" to amountPkr,
                        "title" to txTitle,
                        "subtitle" to txSubtitle,
                        "referenceId" to wdRef.id,
                        "status" to "pending",
                        "balanceAfterPkr" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    adminTxRef,
                    mapOf(
                        "userId" to uid,
                        "inGameName" to profile.inGameName,
                        "userEmail" to profile.email,
                        "type" to WalletTxType.WITHDRAWAL.raw,
                        "direction" to "debit",
                        "amountPkr" to amountPkr,
                        "title" to txTitle,
                        "subtitle" to txSubtitle,
                        "referenceId" to wdRef.id,
                        "status" to "pending",
                        "balanceAfterPkr" to newBalance,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN WALLET VERIFICATION ---

    suspend fun reviewDepositRequest(
        request: DepositRequest,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        val path = "depositRequests/${request.id}"
        return try {
            val adminUid = requireUserId()
            require(request.id.isNotBlank()) { "Deposit request ID missing." }
            val cleanNote = adminNote.trim().take(300)
            val method = PaymentMethod.fromRaw(request.paymentMethod)

            val depRef = db.collection("depositRequests").document(request.id)
            val userRef = db.collection("users").document(request.userId)
            val txRef = userRef.collection("walletTransactions").document()
            val adminTxRef = db.collection("adminTransactions").document(txRef.id)

            db.runTransaction { tx ->
                val depSnap = tx.get(depRef)
                if (!depSnap.exists()) throw IllegalStateException("Deposit request not found.")
                val liveStatus = depSnap.getString("status") ?: RequestStatus.PENDING.raw
                if (liveStatus != RequestStatus.PENDING.raw) {
                    throw IllegalStateException("This deposit request has already been $liveStatus.")
                }
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("Player account not found.")
                val currentBalance = (userSnap.getLong("walletBalancePkr") ?: 0L).coerceAtLeast(0L)

                val newStatus = if (approve) RequestStatus.APPROVED.raw else RequestStatus.REJECTED.raw
                tx.update(
                    depRef,
                    mapOf(
                        "status" to newStatus,
                        "adminNote" to cleanNote,
                        "reviewedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (approve) {
                    val newBalance = currentBalance + request.amountPkr
                    val txTitle = "${method.displayName} Deposit Approved"
                    val txSubtitle = "TID: ${request.transactionId}".take(150)
                    tx.update(
                        userRef,
                        mapOf(
                            "walletBalancePkr" to newBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to request.userId,
                            "type" to WalletTxType.DEPOSIT.raw,
                            "direction" to "credit",
                            "amountPkr" to request.amountPkr,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to request.id,
                            "status" to "completed",
                            "balanceAfterPkr" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        adminTxRef,
                        mapOf(
                            "userId" to request.userId,
                            "inGameName" to request.inGameName,
                            "userEmail" to request.userEmail,
                            "type" to WalletTxType.DEPOSIT.raw,
                            "direction" to "credit",
                            "amountPkr" to request.amountPkr,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to request.id,
                            "status" to "completed",
                            "balanceAfterPkr" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reviewWithdrawalRequest(
        request: WithdrawalRequest,
        approve: Boolean,
        adminNote: String
    ): Result<Unit> {
        val path = "withdrawalRequests/${request.id}"
        return try {
            val adminUid = requireUserId()
            require(request.id.isNotBlank()) { "Withdrawal request ID missing." }
            val cleanNote = adminNote.trim().take(300)
            val method = PaymentMethod.fromRaw(request.paymentMethod)

            val wdRef = db.collection("withdrawalRequests").document(request.id)
            val userRef = db.collection("users").document(request.userId)
            val txRef = userRef.collection("walletTransactions").document()
            val adminTxRef = db.collection("adminTransactions").document(txRef.id)

            db.runTransaction { tx ->
                val wdSnap = tx.get(wdRef)
                if (!wdSnap.exists()) throw IllegalStateException("Withdrawal request not found.")
                val liveStatus = wdSnap.getString("status") ?: RequestStatus.PENDING.raw
                if (liveStatus != RequestStatus.PENDING.raw) {
                    throw IllegalStateException("This withdrawal request has already been $liveStatus.")
                }
                val userSnap = tx.get(userRef)
                if (!userSnap.exists()) throw IllegalStateException("Player account not found.")
                val currentBalance = (userSnap.getLong("walletBalancePkr") ?: 0L).coerceAtLeast(0L)

                val newStatus = if (approve) RequestStatus.APPROVED.raw else RequestStatus.REJECTED.raw
                tx.update(
                    wdRef,
                    mapOf(
                        "status" to newStatus,
                        "adminNote" to cleanNote,
                        "reviewedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (approve) {
                    val noteSuffix = if (cleanNote.isNotBlank()) " • $cleanNote" else ""
                    val txTitle = "${method.displayName} Payout Approved"
                    val txSubtitle = "To: ${request.accountNumber}$noteSuffix".take(150)
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to request.userId,
                            "type" to WalletTxType.WITHDRAWAL.raw,
                            "direction" to "debit",
                            "amountPkr" to request.amountPkr,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to request.id,
                            "status" to "completed",
                            "balanceAfterPkr" to currentBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        adminTxRef,
                        mapOf(
                            "userId" to request.userId,
                            "inGameName" to request.inGameName,
                            "userEmail" to request.userEmail,
                            "type" to WalletTxType.WITHDRAWAL.raw,
                            "direction" to "debit",
                            "amountPkr" to request.amountPkr,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to request.id,
                            "status" to "completed",
                            "balanceAfterPkr" to currentBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val refundedBalance = currentBalance + request.amountPkr
                    val txTitle = "Withdrawal Refunded"
                    val txSubtitle = cleanNote.ifBlank { "Rejected by admin verification" }.take(150)
                    tx.update(
                        userRef,
                        mapOf(
                            "walletBalancePkr" to refundedBalance,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to request.userId,
                            "type" to WalletTxType.WITHDRAWAL_REFUND.raw,
                            "direction" to "credit",
                            "amountPkr" to request.amountPkr,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to request.id,
                            "status" to "completed",
                            "balanceAfterPkr" to refundedBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        adminTxRef,
                        mapOf(
                            "userId" to request.userId,
                            "inGameName" to request.inGameName,
                            "userEmail" to request.userEmail,
                            "type" to WalletTxType.WITHDRAWAL_REFUND.raw,
                            "direction" to "credit",
                            "amountPkr" to request.amountPkr,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to request.id,
                            "status" to "completed",
                            "balanceAfterPkr" to refundedBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN TOURNAMENT CRUD ---

    suspend fun createTournament(
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
    ): Result<Unit> {
        var path = "tournaments"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanMap = mapName.trim()
            val cleanTime = matchTime.trim()
            val cleanRules = rules.trim()
            require(cleanTitle.length in 3..80) { "Title must be 3-80 characters." }
            require(cleanMap.length in 2..40) { "Map name must be 2-40 characters." }
            require(cleanTime.length in 2..60) { "Match time must be 2-60 characters." }
            require(cleanRules.length in 5..2000) { "Rules must be 5-2000 characters." }
            require(totalSlots in 2L..100L) { "Total slots must be between 2 and 100." }
            require(entryFeePkr in 0L..100000L) { "Entry fee out of valid bounds." }
            require(prizePoolPkr in 0L..10000000L) { "Prize pool out of valid bounds." }

            val docRef = db.collection("tournaments").document()
            path = docRef.path
            val payload = mapOf(
                "title" to cleanTitle,
                "mode" to mode.label,
                "status" to status.raw,
                "mapName" to cleanMap,
                "entryFeePkr" to entryFeePkr,
                "prizePoolPkr" to prizePoolPkr,
                "firstPrizePkr" to firstPrizePkr.coerceAtLeast(0L),
                "secondPrizePkr" to secondPrizePkr.coerceAtLeast(0L),
                "thirdPrizePkr" to thirdPrizePkr.coerceAtLeast(0L),
                "matchTime" to cleanTime,
                "totalSlots" to totalSlots,
                "registeredCount" to 0L,
                "rules" to cleanRules,
                "roomId" to roomId.trim().take(60),
                "roomPassword" to roomPassword.trim().take(60),
                "winnerName" to "",
                "winnerUid" to "",
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTournament(
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
    ): Result<Unit> {
        var path = "tournaments/$tournamentId"
        return try {
            requireUserId()
            require(tournamentId.isNotBlank()) { "Tournament ID cannot be blank" }
            val cleanTitle = title.trim()
            val cleanMap = mapName.trim()
            val cleanTime = matchTime.trim()
            val cleanRules = rules.trim()
            require(cleanTitle.length in 3..80) { "Title must be 3-80 characters." }
            require(cleanMap.length in 2..40) { "Map name must be 2-40 characters." }
            require(cleanTime.length in 2..60) { "Match time must be 2-60 characters." }
            require(cleanRules.length in 5..2000) { "Rules must be 5-2000 characters." }
            require(totalSlots in 2L..100L) { "Total slots must be between 2 and 100." }

            val docRef = db.collection("tournaments").document(tournamentId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "title" to cleanTitle,
                    "mode" to mode.label,
                    "status" to status.raw,
                    "mapName" to cleanMap,
                    "entryFeePkr" to entryFeePkr.coerceAtLeast(0L),
                    "prizePoolPkr" to prizePoolPkr.coerceAtLeast(0L),
                    "firstPrizePkr" to firstPrizePkr.coerceAtLeast(0L),
                    "secondPrizePkr" to secondPrizePkr.coerceAtLeast(0L),
                    "thirdPrizePkr" to thirdPrizePkr.coerceAtLeast(0L),
                    "matchTime" to cleanTime,
                    "totalSlots" to totalSlots,
                    "rules" to cleanRules,
                    "roomId" to roomId.trim().take(60),
                    "roomPassword" to roomPassword.trim().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTournament(tournamentId: String): Result<Unit> {
        val path = "tournaments/$tournamentId"
        return try {
            requireUserId()
            require(tournamentId.isNotBlank()) { "Tournament ID cannot be blank" }
            db.collection("tournaments").document(tournamentId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN PUBLISH RESULTS, WINNERS & PLAYER SCORING ---

    suspend fun publishPlayerResult(
        tournament: Tournament,
        registration: TournamentRegistration,
        matchPoints: Long,
        killPoints: Long,
        placement: Long,
        markAsWinner: Boolean,
        prizePayoutPkr: Long = 0L
    ): Result<Unit> {
        val path = "tournaments/${tournament.id}/registrations/${registration.userId}"
        return try {
            val adminUid = requireUserId()
            val safeMatchPts = matchPoints.coerceIn(0L, 100000L)
            val safeKillPts = killPoints.coerceIn(0L, 100000L)
            val totalPts = safeMatchPts + safeKillPts
            val safePlacement = placement.coerceIn(0L, 100L)
            val safePrize = prizePayoutPkr.coerceIn(0L, 10_000_000L)

            val deltaMatchPts = safeMatchPts - registration.matchPoints
            val deltaKillPts = safeKillPts - registration.killPoints
            val deltaTotalPts = totalPts - registration.totalPoints
            val isFirstTimeScored = registration.placement == 0L && safePlacement > 0L

            val tourneyRef = db.collection("tournaments").document(tournament.id)
            val tourneyRegRef = tourneyRef.collection("registrations").document(registration.userId)
            val userRef = db.collection("users").document(registration.userId)
            val userRegRef = userRef.collection("registrations").document(tournament.id)
            val lbRef = db.collection("leaderboard").document(registration.userId)
            val txRef = userRef.collection("walletTransactions").document()

            val newStatus = if (markAsWinner) TournamentStatus.COMPLETED.raw else tournament.status

            db.runTransaction { tx ->
                val userSnap = if (safePrize > 0L) tx.get(userRef) else null
                val currentBalance = (userSnap?.getLong("walletBalancePkr") ?: 0L).coerceAtLeast(0L)

                val regUpdates = mapOf(
                    "matchPoints" to safeMatchPts,
                    "killPoints" to safeKillPts,
                    "totalPoints" to totalPts,
                    "placement" to safePlacement,
                    "tournamentStatus" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                tx.update(tourneyRegRef, regUpdates)
                tx.update(userRegRef, regUpdates)

                val statUpdates = mutableMapOf<String, Any>(
                    "matchPoints" to FieldValue.increment(deltaMatchPts),
                    "killPoints" to FieldValue.increment(deltaKillPts),
                    "totalPoints" to FieldValue.increment(deltaTotalPts),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (isFirstTimeScored) {
                    statUpdates["matchesPlayed"] = FieldValue.increment(1L)
                }
                if (markAsWinner) {
                    statUpdates["totalWins"] = FieldValue.increment(1L)
                }
                tx.update(lbRef, statUpdates)

                val userStatUpdates = statUpdates.toMutableMap()
                if (safePrize > 0L) {
                    val newBalance = currentBalance + safePrize
                    val userEmail = userSnap?.getString("email").orEmpty().ifBlank { "${registration.userId}@player.gamerzonepk" }
                    val txTitle = "Prize: ${tournament.title}".take(100)
                    val txSubtitle = "Placement #$safePlacement • $totalPts PTS".take(150)
                    val adminTxRef = db.collection("adminTransactions").document(txRef.id)
                    userStatUpdates["walletBalancePkr"] = newBalance
                    tx.set(
                        txRef,
                        mapOf(
                            "userId" to registration.userId,
                            "type" to WalletTxType.PRIZE_PAYOUT.raw,
                            "direction" to "credit",
                            "amountPkr" to safePrize,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to tournament.id,
                            "status" to "completed",
                            "balanceAfterPkr" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                    tx.set(
                        adminTxRef,
                        mapOf(
                            "userId" to registration.userId,
                            "inGameName" to registration.inGameName,
                            "userEmail" to userEmail,
                            "type" to WalletTxType.PRIZE_PAYOUT.raw,
                            "direction" to "credit",
                            "amountPkr" to safePrize,
                            "title" to txTitle,
                            "subtitle" to txSubtitle,
                            "referenceId" to tournament.id,
                            "status" to "completed",
                            "balanceAfterPkr" to newBalance,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                tx.update(userRef, userStatUpdates)

                if (markAsWinner) {
                    tx.update(
                        tourneyRef,
                        mapOf(
                            "status" to TournamentStatus.COMPLETED.raw,
                            "winnerName" to registration.inGameName,
                            "winnerUid" to registration.userId,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val annRef = db.collection("announcements").document()
                    tx.set(
                        annRef,
                        mapOf(
                            "title" to "BOOYAH! ${registration.inGameName} Wins ${tournament.title}",
                            "message" to "${registration.inGameName} (${registration.teamName}) claimed #1 Victory with $totalPts pts (${tournament.firstPrizePkr} PKR prize)!",
                            "category" to AnnouncementCategory.WINNER.raw,
                            "createdBy" to adminUid,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustLeaderboardPlayerStats(
        entry: LeaderboardEntry,
        matchesDelta: Long,
        winsDelta: Long,
        matchPointsDelta: Long,
        killPointsDelta: Long
    ): Result<Unit> {
        val path = "leaderboard/${entry.userId}"
        return try {
            requireUserId()
            val newMatches = (entry.matchesPlayed + matchesDelta).coerceAtLeast(0L)
            val newWins = (entry.totalWins + winsDelta).coerceAtLeast(0L)
            val newMatchPts = (entry.matchPoints + matchPointsDelta).coerceAtLeast(0L)
            val newKillPts = (entry.killPoints + killPointsDelta).coerceAtLeast(0L)
            val newTotalPts = newMatchPts + newKillPts

            val updates = mapOf(
                "matchesPlayed" to newMatches,
                "totalWins" to newWins,
                "matchPoints" to newMatchPts,
                "killPoints" to newKillPts,
                "totalPoints" to newTotalPts,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            batch.update(db.collection("leaderboard").document(entry.userId), updates)
            batch.update(db.collection("users").document(entry.userId), updates)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createAnnouncement(
        title: String,
        message: String,
        category: AnnouncementCategory
    ): Result<Unit> {
        var path = "announcements"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanMessage = message.trim()
            require(cleanTitle.length in 3..100) { "Title must be 3-100 characters." }
            require(cleanMessage.length in 5..1000) { "Message must be 5-1000 characters." }

            val docRef = db.collection("announcements").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "title" to cleanTitle,
                    "message" to cleanMessage,
                    "category" to category.raw,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
        val path = "announcements/$announcementId"
        return try {
            requireUserId()
            require(announcementId.isNotBlank()) { "Announcement ID cannot be blank" }
            db.collection("announcements").document(announcementId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterTournaments(): Result<Unit> {
        val path = "tournaments"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            val starters = listOf(
                mapOf(
                    "title" to "PK Battle Royale Championship",
                    "mode" to TournamentMode.SQUAD.label,
                    "status" to TournamentStatus.UPCOMING.raw,
                    "mapName" to "Bermuda",
                    "entryFeePkr" to 50L,
                    "prizePoolPkr" to 1750L,
                    "firstPrizePkr" to 1000L,
                    "secondPrizePkr" to 500L,
                    "thirdPrizePkr" to 250L,
                    "matchTime" to "Tonight • 9:00 PM PKT",
                    "totalSlots" to 48L,
                    "registeredCount" to 0L,
                    "rules" to "No hacks, no emulators, no teaming. Level 30+ accounts only. Join custom room 10 mins before start.",
                    "roomId" to "",
                    "roomPassword" to "",
                    "winnerName" to "",
                    "winnerUid" to "",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Lahore Clash Squad 4v4 Showdown",
                    "mode" to TournamentMode.CLASH_SQUAD.label,
                    "status" to TournamentStatus.LIVE.raw,
                    "mapName" to "Kalahari",
                    "entryFeePkr" to 100L,
                    "prizePoolPkr" to 3000L,
                    "firstPrizePkr" to 2000L,
                    "secondPrizePkr" to 1000L,
                    "thirdPrizePkr" to 0L,
                    "matchTime" to "Live Now • Round 2",
                    "totalSlots" to 16L,
                    "registeredCount" to 0L,
                    "rules" to "Best of 7 Clash Squad. Character skills allowed, no grenade spam on spawn.",
                    "roomId" to "GZPK-9921",
                    "roomPassword" to "4488",
                    "winnerName" to "",
                    "winnerUid" to "",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Karachi Solo Rush Cup",
                    "mode" to TournamentMode.SOLO.label,
                    "status" to TournamentStatus.UPCOMING.raw,
                    "mapName" to "Purgatory",
                    "entryFeePkr" to 30L,
                    "prizePoolPkr" to 1200L,
                    "firstPrizePkr" to 700L,
                    "secondPrizePkr" to 350L,
                    "thirdPrizePkr" to 150L,
                    "matchTime" to "Tomorrow • 8:00 PM PKT",
                    "totalSlots" to 48L,
                    "registeredCount" to 0L,
                    "rules" to "Solo Battle Royale. 10 pts per Booyah, 2 pts per kill. Mobile devices only.",
                    "roomId" to "",
                    "roomPassword" to "",
                    "winnerName" to "",
                    "winnerUid" to "",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Islamabad Duo Arena",
                    "mode" to TournamentMode.DUO.label,
                    "status" to TournamentStatus.UPCOMING.raw,
                    "mapName" to "Alpine",
                    "entryFeePkr" to 80L,
                    "prizePoolPkr" to 2500L,
                    "firstPrizePkr" to 1500L,
                    "secondPrizePkr" to 700L,
                    "thirdPrizePkr" to 300L,
                    "matchTime" to "Friday • 9:30 PM PKT",
                    "totalSlots" to 24L,
                    "registeredCount" to 0L,
                    "rules" to "Duo queue on Alpine. Screenshots of scoreboard required for verification.",
                    "roomId" to "",
                    "roomPassword" to "",
                    "winnerName" to "",
                    "winnerUid" to "",
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            starters.forEach { payload ->
                batch.set(db.collection("tournaments").document(), payload)
            }
            val annRef = db.collection("announcements").document()
            batch.set(
                annRef,
                mapOf(
                    "title" to "Welcome to GAMER ZONE PK Season 1!",
                    "message" to "Daily Solo, Duo, Squad & Clash Squad Free Fire tournaments are now open. Register your Free Fire UID to compete for PKR prizes!",
                    "category" to AnnouncementCategory.TOURNAMENT.raw,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
