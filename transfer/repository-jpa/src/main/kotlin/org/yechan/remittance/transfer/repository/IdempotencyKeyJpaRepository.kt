package org.yechan.remittance.transfer.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update IdempotencyKeyEntity i
           set i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS,
               i.requestHash = :requestHash,
               i.startedAt = :startedAt
         where i.memberId = :memberId
           and i.scope = :scope
           and i.idempotencyKey = :idempotencyKey
           and i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START
        """,
    )
    fun markInProgressIfBeforeStart(
        @Param("memberId") memberId: Long,
        @Param("scope") scope: IdempotencyKeyProps.IdempotencyScopeValue,
        @Param("idempotencyKey") idempotencyKey: String,
        @Param("requestHash") requestHash: String,
        @Param("startedAt") startedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update IdempotencyKeyEntity i
           set i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED,
               i.responseSnapshot = :responseSnapshot,
               i.completedAt = :completedAt
         where i.memberId = :memberId
           and i.scope = :scope
           and i.idempotencyKey = :idempotencyKey
           and i.requestHash = :requestHash
           and i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS
        """,
    )
    fun markSucceededIfInProgress(
        @Param("memberId") memberId: Long,
        @Param("scope") scope: IdempotencyKeyProps.IdempotencyScopeValue,
        @Param("idempotencyKey") idempotencyKey: String,
        @Param("requestHash") requestHash: String,
        @Param("responseSnapshot") responseSnapshot: String,
        @Param("completedAt") completedAt: LocalDateTime,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update IdempotencyKeyEntity i
           set i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.FAILED,
               i.responseSnapshot = :responseSnapshot,
               i.completedAt = :completedAt
         where i.memberId = :memberId
           and i.scope = :scope
           and i.idempotencyKey = :idempotencyKey
           and i.requestHash = :requestHash
           and i.status = org.yechan.remittance.transfer.IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS
        """,
    )
    fun markFailedIfInProgress(
        @Param("memberId") memberId: Long,
        @Param("scope") scope: IdempotencyKeyProps.IdempotencyScopeValue,
        @Param("idempotencyKey") idempotencyKey: String,
        @Param("requestHash") requestHash: String,
        @Param("responseSnapshot") responseSnapshot: String,
        @Param("completedAt") completedAt: LocalDateTime,
    ): Int
}
