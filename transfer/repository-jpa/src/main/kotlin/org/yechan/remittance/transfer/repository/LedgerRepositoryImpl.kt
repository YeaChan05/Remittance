package org.yechan.remittance.transfer.repository

import org.yechan.remittance.transfer.LedgerModel
import org.yechan.remittance.transfer.LedgerProps
import org.yechan.remittance.transfer.LedgerRepository
import java.math.BigDecimal
import java.time.LocalDateTime

class LedgerRepositoryImpl(
    private val repository: LedgerJpaRepository,
) : LedgerRepository {
    override fun save(props: LedgerProps): LedgerModel = repository.save(LedgerEntity.create(props))

    override fun existsByTransferIdAndAccountIdAndSide(
        transferId: Long,
        accountId: Long,
        side: LedgerProps.LedgerSideValue,
    ): Boolean = repository.existsByTransferIdAndAccountIdAndSide(transferId, accountId, side)

    override fun sumAmountByAccountIdAndSideBetween(
        accountId: Long,
        side: LedgerProps.LedgerSideValue,
        from: LocalDateTime,
        to: LocalDateTime,
    ): BigDecimal = repository.sumAmountByAccountIdAndSideBetween(accountId, side, from, to)
}
