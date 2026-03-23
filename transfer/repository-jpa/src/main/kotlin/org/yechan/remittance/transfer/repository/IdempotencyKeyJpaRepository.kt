package org.yechan.remittance.transfer.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.yechan.remittance.transfer.IdempotencyKeyProps
import java.time.LocalDateTime

interface IdempotencyKeyJpaRepository : JpaRepository<IdempotencyKeyEntity, Long> {
    fun findByMemberIdAndScopeAndIdempotencyKey(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
    ): IdempotencyKeyEntity?

    fun findByStatusAndStartedAtBefore(
        status: IdempotencyKeyProps.IdempotencyKeyStatusValue,
        startedAt: LocalDateTime,
    ): List<IdempotencyKeyEntity>
}
