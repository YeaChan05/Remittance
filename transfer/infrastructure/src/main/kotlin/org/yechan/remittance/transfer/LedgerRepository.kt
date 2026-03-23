package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime

interface LedgerRepository {
    fun save(props: LedgerProps): LedgerModel

    fun existsByTransferIdAndAccountIdAndSide(
        transferId: Long,
        accountId: Long,
        side: LedgerProps.LedgerSideValue,
    ): Boolean

    fun sumAmountByAccountIdAndSideBetween(
        accountId: Long,
        side: LedgerProps.LedgerSideValue,
        from: LocalDateTime,
        to: LocalDateTime,
    ): BigDecimal
}
