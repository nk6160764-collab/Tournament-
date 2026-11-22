package com.example

import com.example.model.PaymentMethod
import com.example.model.RequestStatus
import com.example.model.TournamentMode
import com.example.model.TournamentStatus
import com.example.model.WalletLimits
import com.example.model.WalletTxType
import org.junit.Assert.assertEquals
import org.junit.Test

class GamerZoneModelsTest {

    @Test
    fun `payment methods match official Pakistani wallet numbers`() {
        assertEquals("03489674834", PaymentMethod.EASYPAISA.accountNumber)
        assertEquals("03406690336", PaymentMethod.JAZZCASH.accountNumber)
        assertEquals(PaymentMethod.EASYPAISA, PaymentMethod.fromRaw("easypaisa"))
        assertEquals(PaymentMethod.JAZZCASH, PaymentMethod.fromRaw("JazzCash"))
    }

    @Test
    fun `wallet limits enforce minimum deposit 100 and minimum withdrawal 200`() {
        assertEquals(100L, WalletLimits.MIN_DEPOSIT_PKR)
        assertEquals(200L, WalletLimits.MIN_WITHDRAWAL_PKR)
    }

    @Test
    fun `tournament modes and statuses resolve safely`() {
        assertEquals(TournamentMode.CLASH_SQUAD, TournamentMode.fromRaw("Clash Squad"))
        assertEquals(TournamentMode.BATTLE_ROYALE, TournamentMode.fromRaw("Battle Royale"))
        assertEquals(TournamentStatus.LIVE, TournamentStatus.fromRaw("live"))
        assertEquals(RequestStatus.APPROVED, RequestStatus.fromRaw("approved"))
        assertEquals(WalletTxType.TOURNAMENT_ENTRY, WalletTxType.fromRaw("tournament_entry"))
    }
}
