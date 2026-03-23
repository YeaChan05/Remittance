package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Clock
import java.time.LocalDateTime

fun interface TransferCreateUseCase {
    fun transfer(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps,
    ): TransferResult
}

private val log = KotlinLogging.logger {}

class TransferService(
    private val idempotencyHandler: TransferIdempotencyHandler,
    private val transferProcessService: TransferProcessService,
    private val ledgerWriter: LedgerWriter,
    private val transferSnapshotUtil: TransferSnapshotUtil,
    private val clock: Clock,
) : TransferCreateUseCase {
    override fun transfer(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps,
    ): TransferResult {
        log.info { "transfer.start memberId=$memberId scope=${props.scope}" }
        val now = LocalDateTime.now(clock)
        val scope = props.toIdempotencyScope()

        // 1. 멱등키와 현재 요청 body 해시를 기준으로 재실행 가능 여부를 확인
        val key = idempotencyHandler.loadKey(memberId, idempotencyKey, scope, now)
        val requestHash = transferSnapshotUtil.toHashRequest(props)

        if (key.isInvalidRequestHash(requestHash)) {
            log.warn { "transfer.idempotency.conflict memberId=$memberId scope=$scope" }
            throw TransferIdempotencyKeyConflictException("Idempotency key conflict")
        }

        // 2. 이번 요청이 실제 처리 권한을 선점하지 못하면 저장된 결과를 그대로 반환
        val marked = idempotencyHandler.markInProgress(memberId, idempotencyKey, scope, requestHash, now)

        if (!marked) {
            log.info { "transfer.idempotency.existing memberId=$memberId scope=$scope" }
            return idempotencyHandler.resolveExisting(memberId, idempotencyKey, scope, requestHash)
        }

        // 3. 선점에 성공한 요청만 본 이체 처리와 멱등 상태 갱신
        val result =
            try {
                transferProcessService.process(memberId, idempotencyKey, props, now)
            } catch (ex: TransferFailedException) {
                log.warn { "transfer.process.failed memberId=$memberId scope=$scope code=${ex.failureCode}" }
                val failed = TransferResult.failed(ex.failureCode)
                idempotencyHandler.markFailed(memberId, idempotencyKey, scope, failed, now)
                return failed
            }

        // 4. 이체 본 처리 후에는 별도 단계로 ledger를 기록하고 기술 실패만 예외처리
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
