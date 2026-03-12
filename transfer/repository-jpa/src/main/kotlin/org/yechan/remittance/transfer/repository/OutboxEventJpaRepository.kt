package org.yechan.remittance.transfer.repository

import java.time.LocalDateTime
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.yechan.remittance.transfer.OutboxEventProps

interface OutboxEventJpaRepository : JpaRepository<OutboxEventEntity, Long> {
    @Query(
        """
        select o from OutboxEventEntity o
        where o.status = :status
          and (:before is null or o.createdAt <= :before)
        order by o.createdAt asc
        """
    )
    fun findNewForPublish(
        @Param("status") status: OutboxEventProps.OutboxEventStatusValue,
        @Param("before") before: LocalDateTime?,
        pageable: Pageable
    ): List<OutboxEventEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update OutboxEventEntity o
        set o.status = org.yechan.remittance.transfer.OutboxEventProps.OutboxEventStatusValue.SENT
        where o.id = :eventId
        """
    )
    fun markSent(@Param("eventId") eventId: Long): Int
}
