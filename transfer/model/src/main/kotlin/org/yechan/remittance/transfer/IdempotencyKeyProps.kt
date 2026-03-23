package org.yechan.remittance.transfer

import java.time.LocalDateTime

interface IdempotencyKeyProps {
    val memberId: Long
    val idempotencyKey: String
    val expiresAt: LocalDateTime?
    val scope: IdempotencyScopeValue
    val status: IdempotencyKeyStatusValue?
    val requestHash: String?
    val responseSnapshot: String?
    val startedAt: LocalDateTime?
    val completedAt: LocalDateTime?

    enum class IdempotencyKeyStatusValue {
        BEFORE_START,
        IN_PROGRESS,
        SUCCEEDED,
        FAILED,
        TIMEOUT,
    }

    enum class IdempotencyScopeValue {
        TRANSFER,
        DEPOSIT,
        WITHDRAW,
    }
}
