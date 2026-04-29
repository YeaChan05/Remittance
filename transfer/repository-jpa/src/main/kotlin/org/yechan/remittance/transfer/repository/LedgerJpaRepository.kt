package org.yechan.remittance.transfer.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.yechan.remittance.transfer.LedgerProps
import java.math.BigDecimal
import java.time.LocalDateTime

interface LedgerJpaRepository : JpaRepository<LedgerEntity, Long> {
    fun existsByTransferIdAndAccountIdAndSide(
        transferId: Long,
        accountId: Long,
        side: LedgerProps.LedgerSideValue,
    ): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        nativeQuery = true,
        value = """
            insert ignore into core.ledger (id, transfer_id, account_id, amount, side, created_at, updated_at)
            values (:id, :transferId, :accountId, :amount, :side, :createdAt, :createdAt)
        """,
    )
    fun insertIfAbsent(
        @Param("id") id: Long,
        @Param("transferId") transferId: Long,
        @Param("accountId") accountId: Long,
        @Param("amount") amount: BigDecimal,
        @Param("side") side: String,
        @Param("createdAt") createdAt: LocalDateTime?,
    ): Int

    @Query(
        """
        select coalesce(sum(l.persistedAmount), 0)
        from LedgerEntity l
        where l.accountId = :accountId
          and l.side = :side
          and l.createdAt >= :from
          and l.createdAt < :to
        """,
    )
    fun sumAmountByAccountIdAndSideBetween(
        @Param("accountId") accountId: Long,
        @Param("side") side: LedgerProps.LedgerSideValue,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): BigDecimal
}
