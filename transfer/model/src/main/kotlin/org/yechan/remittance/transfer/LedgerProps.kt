package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime

interface LedgerProps {
    val transferId: Long
    val accountId: Long
    val amount: BigDecimal
    val side: LedgerSideValue
    val createdAt: LocalDateTime?

    enum class LedgerSideValue {
        DEBIT,
        CREDIT,
    }
}
