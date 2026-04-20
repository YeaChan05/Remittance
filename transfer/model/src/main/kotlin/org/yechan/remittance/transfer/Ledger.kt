package org.yechan.remittance.transfer

import org.yechan.remittance.Money
import java.time.LocalDateTime

data class Ledger(
    override val ledgerId: Long?,
    override val transferId: Long,
    override val accountId: Long,
    override val amount: Money,
    override val side: LedgerProps.LedgerSideValue,
    override val createdAt: LocalDateTime?,
) : LedgerModel
