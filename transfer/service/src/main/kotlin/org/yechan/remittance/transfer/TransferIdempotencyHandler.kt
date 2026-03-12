package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

open class TransferIdempotencyHandler(
    private val repository: IdempotencyKeyRepository,
    private val transferSnapshotUtil: TransferSnapshotUtil
) {
    open fun loadKey(
        memberId: Long,
        idempotencyKey: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        now: LocalDateTime
    ): IdempotencyKeyModel {
        log.info { "transfer.idempotency.load memberId=$memberId scope=$scope" }
        val key = getIdempotencyKey(memberId, idempotencyKey, scope)
        if (key.isExpired(now)) {
            log.warn { "transfer.idempotency.expired memberId=$memberId scope=$scope" }
            throw TransferIdempotencyKeyExpiredException("Idempotency key expired")
        }
        return key
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun markInProgress(
        memberId: Long,
        idempotencyKey: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        requestHash: String,
        now: LocalDateTime
    ): Boolean {
        log.debug { "transfer.idempotency.mark_in_progress memberId=$memberId scope=$scope" }
        return repository.tryMarkInProgress(memberId, scope, idempotencyKey, requestHash, now)
    }

    open fun resolveExisting(
        memberId: Long,
        idempotencyKey: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        requestHash: String?
    ): TransferResult {
        log.info { "transfer.idempotency.resolve memberId=$memberId scope=$scope" }
        val existing = getIdempotencyKey(memberId, idempotencyKey, scope)

        if (existing.isInvalidRequestHash(requestHash)) {
            log.warn { "transfer.idempotency.conflict memberId=$memberId scope=$scope" }
            throw TransferIdempotencyKeyConflictException("Idempotency key conflict")
        }

        if (existing.status == IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS) {
            log.info { "transfer.idempotency.in_progress memberId=$memberId scope=$scope" }
            return TransferResult.inProgress()
        }

        val responseSnapshot = existing.responseSnapshot
        if (responseSnapshot == null) {
            log.info { "transfer.idempotency.no_snapshot memberId=$memberId scope=$scope" }
            return TransferResult.inProgress()
        }

        return transferSnapshotUtil.fromSnapshot(responseSnapshot)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun markFailed(
        memberId: Long,
        idempotencyKey: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue,
        failed: TransferResult,
        now: LocalDateTime
    ) {
        log.warn { "transfer.idempotency.mark_failed memberId=$memberId scope=$scope" }
        repository.markFailed(
            memberId,
            scope,
            idempotencyKey,
            transferSnapshotUtil.toSnapshot(failed),
            now
        )
    }

    private fun getIdempotencyKey(
        memberId: Long,
        idempotencyKey: String,
        scope: IdempotencyKeyProps.IdempotencyScopeValue
    ): IdempotencyKeyModel {
        return repository.findByKey(memberId, scope, idempotencyKey)
            ?: throw TransferIdempotencyKeyNotFoundException("Idempotency key not found")
    }
}
