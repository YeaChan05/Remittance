package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime

data class Ledger(
    override val ledgerId: Long?,
    override val transferId: Long,
    override val accountId: Long,
    override val amount: BigDecimal,
    override val side: LedgerProps.LedgerSideValue,
    override val createdAt: LocalDateTime?,
) : LedgerModel
