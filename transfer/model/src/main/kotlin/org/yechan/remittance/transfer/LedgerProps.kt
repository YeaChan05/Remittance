package org.yechan.remittance.transfer

import org.yechan.remittance.Money
import java.time.LocalDateTime

interface LedgerProps {
    val transferId: Long
    val accountId: Long
    val amount: Money
    val side: LedgerSideValue
    val createdAt: LocalDateTime?

    enum class LedgerSideValue {
        DEBIT,
        CREDIT,
    }
}
