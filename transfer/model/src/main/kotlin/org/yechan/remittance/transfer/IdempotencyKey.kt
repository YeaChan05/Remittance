package org.yechan.remittance.transfer

import java.time.LocalDateTime

data class IdempotencyKey(
    override val idempotencyKeyId: Long?,
    override val memberId: Long,
    override val idempotencyKey: String,
    override val expiresAt: LocalDateTime,
    override val scope: IdempotencyKeyProps.IdempotencyScopeValue,
    override val status: IdempotencyKeyProps.IdempotencyKeyStatusValue,
    override val requestHash: String?,
    override val responseSnapshot: String?,
    override val startedAt: LocalDateTime?,
    override val completedAt: LocalDateTime?
) : IdempotencyKeyModel
