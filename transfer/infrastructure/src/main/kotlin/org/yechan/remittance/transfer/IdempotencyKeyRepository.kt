package org.yechan.remittance.transfer

import java.time.LocalDateTime

interface IdempotencyKeyRepository {
    fun save(props: IdempotencyKeyProps): IdempotencyKeyModel

    fun findByKey(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
    ): IdempotencyKeyModel?

    fun tryMarkInProgress(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
        requestHash: String,
        startedAt: LocalDateTime,
    ): Boolean

    fun markSucceeded(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
        responseSnapshot: String,
        completedAt: LocalDateTime,
    ): IdempotencyKeyModel

    fun markFailed(
        memberId: Long,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        idempotencyKey: String,
        responseSnapshot: String,
        completedAt: LocalDateTime,
    ): IdempotencyKeyModel

    fun markTimeoutBefore(
        cutoff: LocalDateTime,
        responseSnapshot: String,
    ): Int
}
