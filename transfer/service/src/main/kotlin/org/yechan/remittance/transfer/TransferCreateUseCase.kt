package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.LocalDateTime

fun interface TransferCreateUseCase {
    fun transfer(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps
    ): TransferResult
}

class TransferService(
    private val idempotencyHandler: TransferIdempotencyHandler,
    private val transferProcessService: TransferProcessService,
    private val ledgerWriter: LedgerWriter,
    private val transferSnapshotUtil: TransferSnapshotUtil,
    private val clock: Clock
) : TransferCreateUseCase {
    private val log = KotlinLogging.logger {}

    override fun transfer(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps
    ): TransferResult {
        log.info { "transfer.start memberId=$memberId scope=${props.scope}" }
        val now = LocalDateTime.now(clock)
        val scope = props.toIdempotencyScope()
        val key = idempotencyHandler.loadKey(memberId, idempotencyKey, scope, now)
        val requestHash = transferSnapshotUtil.toHashRequest(props)

        if (key.isInvalidRequestHash(requestHash)) {
            log.warn { "transfer.idempotency.conflict memberId=$memberId scope=$scope" }
            throw TransferIdempotencyKeyConflictException("Idempotency key conflict")
        }

        val marked = idempotencyHandler.markInProgress(memberId, idempotencyKey, scope, requestHash, now)

        if (!marked) {
            log.info { "transfer.idempotency.existing memberId=$memberId scope=$scope" }
            return idempotencyHandler.resolveExisting(memberId, idempotencyKey, scope, requestHash)
        }

        val result =
            try {
                transferProcessService.process(memberId, idempotencyKey, props, now)
            } catch (ex: TransferFailedException) {
                log.warn { "transfer.process.failed memberId=$memberId scope=$scope code=${ex.failureCode}" }
                val failed = TransferResult.failed(ex.failureCode)
                idempotencyHandler.markFailed(memberId, idempotencyKey, scope, failed, now)
                return failed
            }

        try {
            ledgerWriter.record(props, result, now)
        } catch (ex: RuntimeException) {
            log.error(ex) {
                "transfer.ledger.record_failed memberId=$memberId transferId=${result.transferId}"
            }
            throw TransferLedgerRecordFailedException("Ledger record failed", ex)
        }

        log.info { "transfer.success memberId=$memberId transferId=${result.transferId}" }
        return result
    }
}
