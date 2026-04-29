package org.yechan.remittance.transfer.repository

import org.yechan.remittance.transfer.IdempotencyKeyModel
import org.yechan.remittance.transfer.IdempotencyKeyProps
import org.yechan.remittance.transfer.IdempotencyKeyRepository
import java.time.LocalDateTime

class IdempotencyKeyRepositoryImpl(
    private val repository: IdempotencyKeyJpaRepository,
) : IdempotencyKeyRepository {
    override fun save(props: IdempotencyKeyProps): IdempotencyKeyModel = repository.save(IdempotencyKeyEntity.create(props))

    override fun findByKey(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
    ): IdempotencyKeyModel? = repository.findByMemberIdAndScopeAndIdempotencyKey(memberId, scope, idempotencyKey)

    override fun tryMarkInProgress(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
        requestHash: String,
        startedAt: LocalDateTime,
    ): Boolean = repository.markInProgressIfBeforeStart(
        memberId,
        scope,
        idempotencyKey,
        requestHash,
        startedAt,
    ) == 1

    override fun markSucceeded(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
        requestHash: String,
        responseSnapshot: String,
        completedAt: LocalDateTime,
    ): IdempotencyKeyModel {
        repository.markSucceededIfInProgress(
            memberId,
            scope,
            idempotencyKey,
            requestHash,
            responseSnapshot,
            completedAt,
        )
        return repository.findByMemberIdAndScopeAndIdempotencyKey(memberId, scope, idempotencyKey)
            ?: throw IllegalStateException("Idempotency key not found")
    }

    override fun markFailed(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
        requestHash: String,
        responseSnapshot: String,
        completedAt: LocalDateTime,
    ): IdempotencyKeyModel {
        repository.markFailedIfInProgress(
            memberId,
            scope,
            idempotencyKey,
            requestHash,
            responseSnapshot,
            completedAt,
        )
        return repository.findByMemberIdAndScopeAndIdempotencyKey(memberId, scope, idempotencyKey)
            ?: throw IllegalStateException("Idempotency key not found")
    }

    override fun markTimeoutBefore(cutoff: LocalDateTime, responseSnapshot: String): Int {
        val completedAt = LocalDateTime.now()
        val candidates = repository.findByStatusAndStartedAtBefore(
            IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS,
            cutoff,
        )
        val updated =
            candidates.count { it.markTimeoutIfBefore(cutoff, responseSnapshot, completedAt) }
        repository.saveAll(candidates)
        return updated
    }
}
