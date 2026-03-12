package org.yechan.remittance.transfer.repository

import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.yechan.remittance.transfer.TransferProps

interface TransferJpaRepository : JpaRepository<TransferEntity, Long> {
    @Query(
        """
        select t from TransferEntity t
        where (t.fromAccountId = :accountId or t.toAccountId = :accountId)
          and t.status in :statuses
          and (:from is null or t.completedAt >= :from)
          and (:to is null or t.completedAt <= :to)
        order by t.completedAt desc
        """
    )
    fun findCompletedByAccountId(
        @Param("accountId") accountId: Long,
        @Param("statuses") statuses: List<TransferProps.TransferStatusValue>,
        @Param("from") from: LocalDateTime?,
        @Param("to") to: LocalDateTime?,
        pageable: Pageable
    ): List<TransferEntity>

    @Query(
        """
        select coalesce(sum(t.amount), 0)
          from TransferEntity t
         where t.fromAccountId = :accountId
           and t.scope = :scope
           and t.status = :status
           and t.requestedAt >= :from
           and t.requestedAt < :to
        """
    )
    fun sumAmountByFromAccountIdAndScopeBetween(
        @Param("accountId") accountId: Long,
        @Param("scope") scope: TransferProps.TransferScopeValue,
        @Param("status") status: TransferProps.TransferStatusValue,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime
    ): BigDecimal
}
