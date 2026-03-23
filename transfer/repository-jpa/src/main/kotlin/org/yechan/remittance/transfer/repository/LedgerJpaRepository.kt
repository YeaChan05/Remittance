package org.yechan.remittance.transfer.repository

import org.springframework.data.jpa.repository.JpaRepository
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

    @Query(
        """
        select coalesce(sum(l.amount), 0)
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
