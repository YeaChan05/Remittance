package org.yechan.remittance.transfer.repository

import java.math.BigDecimal
import java.time.LocalDateTime
import org.yechan.remittance.transfer.LedgerModel
import org.yechan.remittance.transfer.LedgerProps
import org.yechan.remittance.transfer.LedgerRepository

class LedgerRepositoryImpl(
    private val repository: LedgerJpaRepository
) : LedgerRepository {
    override fun save(props: LedgerProps): LedgerModel {
        return repository.save(LedgerEntity.create(props))
    }

    override fun existsByTransferIdAndAccountIdAndSide(
        transferId: Long,
        accountId: Long,
        side: LedgerProps.LedgerSideValue
    ): Boolean {
        return repository.existsByTransferIdAndAccountIdAndSide(transferId, accountId, side)
    }

    override fun sumAmountByAccountIdAndSideBetween(
        accountId: Long,
        side: LedgerProps.LedgerSideValue,
        from: LocalDateTime,
        to: LocalDateTime
    ): BigDecimal {
        return repository.sumAmountByAccountIdAndSideBetween(accountId, side, from, to)
    }
}
