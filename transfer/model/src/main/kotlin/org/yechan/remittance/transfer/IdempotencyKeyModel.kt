package org.yechan.remittance.transfer

import java.time.LocalDateTime

interface IdempotencyKeyModel : IdempotencyKeyProps, IdempotencyKeyIdentifier {
    fun tryMarkInProgress(
        requestHash: String,
        startedAt: LocalDateTime?
    ): Boolean {
        throw UnsupportedOperationException("Try mark in progress not supported")
    }

    fun markSucceeded(
        responseSnapshot: String,
        completedAt: LocalDateTime
    ) {
        throw UnsupportedOperationException("Mark succeeded not supported")
    }

    fun markFailed(
        responseSnapshot: String,
        completedAt: LocalDateTime
    ) {
        throw UnsupportedOperationException("Mark failed not supported")
    }

    fun markTimeoutIfBefore(
        cutoff: LocalDateTime,
        responseSnapshot: String,
        completedAt: LocalDateTime
    ): Boolean {
        throw UnsupportedOperationException("Mark timeout not supported")
    }

    fun isInvalidRequestHash(requestHash: String?): Boolean {
        if (this.requestHash == null || requestHash == null) {
            return false
        }
        return this.requestHash != requestHash
    }

    fun isExpired(now: LocalDateTime): Boolean {
        return expiresAt?.isBefore(now) == true
    }
}
