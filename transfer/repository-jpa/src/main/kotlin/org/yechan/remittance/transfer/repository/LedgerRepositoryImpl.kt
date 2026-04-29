package org.yechan.remittance.transfer.repository

import org.yechan.remittance.Money
import org.yechan.remittance.transfer.LedgerModel
import org.yechan.remittance.transfer.LedgerProps
import org.yechan.remittance.transfer.LedgerRepository
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom

class LedgerRepositoryImpl(
    private val repository: LedgerJpaRepository,
) : LedgerRepository {
    override fun save(props: LedgerProps): LedgerModel = repository.save(LedgerEntity.create(props))

    override fun saveIfAbsent(props: LedgerProps): Boolean = repository.insertIfAbsent(
        id = nextId(),
        transferId = props.transferId,
        accountId = props.accountId,
        amount = props.amount.amount,
        side = props.side.name,
        createdAt = LocalDateTime.now(),
    ) == INSERTED_ROW_COUNT

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
    ): Money = Money.of(repository.sumAmountByAccountIdAndSideBetween(accountId, side, from, to))

    private companion object {
        const val INSERTED_ROW_COUNT = 1

        fun nextId(): Long = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE)
    }
}
