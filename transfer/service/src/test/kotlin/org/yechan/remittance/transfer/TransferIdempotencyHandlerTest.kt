package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TransferIdempotencyHandlerTest {
    @Test
    fun `멱등키가 없으면 예외를 던진다`() {
        val handler = TransferIdempotencyHandler(
            RecordingKeyRepository(key = null),
            TransferSnapshotUtil(ObjectMapper()),
        )

        assertThatThrownBy {
            handler.loadKey(1L, "k", IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER, now())
        }.isInstanceOf(TransferIdempotencyKeyNotFoundException::class.java)
    }

    @Test
    fun `만료된 멱등키 조회는 예외를 던진다`() {
        val handler = TransferIdempotencyHandler(
            RecordingKeyRepository(
                TestKey(
                    status = IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START,
                    expiresAt = LocalDateTime.parse("2026-01-01T09:00:00"),
                    responseSnapshot = null,
                ),
            ),
            TransferSnapshotUtil(ObjectMapper()),
        )

        assertThatThrownBy {
            handler.loadKey(1L, "k", IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER, now())
        }.isInstanceOf(TransferIdempotencyKeyExpiredException::class.java)
    }

    @Test
    fun `markInProgress는 repository 결과 true를 그대로 반환한다`() {
        val repository = RecordingKeyRepository(
            key = TestKey(
                status = IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START,
                expiresAt = null,
                responseSnapshot = null,
            ),
            markInProgressResult = true,
        )
        val handler = TransferIdempotencyHandler(repository, TransferSnapshotUtil(ObjectMapper()))

        val result = handler.markInProgress(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            "hash",
            now(),
        )

        assertThat(result).isTrue()
        assertThat(repository.markInProgressArgs).isEqualTo(
            MarkInProgressArgs(
                memberId = 1L,
                scope = IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
                idempotencyKey = "k",
                requestHash = "hash",
                startedAt = now(),
            ),
        )
    }

    @Test
    fun `markInProgress는 repository 결과 false를 그대로 반환한다`() {
        val repository = RecordingKeyRepository(
            key = TestKey(
                status = IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START,
                expiresAt = null,
                responseSnapshot = null,
            ),
            markInProgressResult = false,
        )
        val handler = TransferIdempotencyHandler(repository, TransferSnapshotUtil(ObjectMapper()))

        val result = handler.markInProgress(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            "hash",
            now(),
        )

        assertThat(result).isFalse()
        assertThat(repository.markInProgressArgs?.requestHash).isEqualTo("hash")
    }

    @Test
    fun `요청 해시가 다르면 충돌 예외를 던진다`() {
        val key =
            TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START, null, null).apply {
                requestHash = "hash"
            }
        val handler = TransferIdempotencyHandler(
            RecordingKeyRepository(key),
            TransferSnapshotUtil(ObjectMapper()),
        )

        assertThatThrownBy {
            handler.resolveExisting(
                1L,
                "k",
                IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
                "other",
            )
        }.isInstanceOf(TransferIdempotencyKeyConflictException::class.java)
    }

    @Test
    fun `IN_PROGRESS 상태면 진행 중 결과를 반환한다`() {
        val handler = TransferIdempotencyHandler(
            RecordingKeyRepository(
                TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS, null, null),
            ),
            TransferSnapshotUtil(ObjectMapper()),
        )

        val result = handler.resolveExisting(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            null,
        )

        assertThat(result.status).isEqualTo(TransferProps.TransferStatusValue.IN_PROGRESS)
    }

    @Test
    fun `스냅샷이 없으면 진행 중 결과를 반환한다`() {
        val handler = TransferIdempotencyHandler(
            RecordingKeyRepository(
                TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED, null, null),
            ),
            TransferSnapshotUtil(ObjectMapper()),
        )

        val result = handler.resolveExisting(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            null,
        )

        assertThat(result.status).isEqualTo(TransferProps.TransferStatusValue.IN_PROGRESS)
    }

    @Test
    fun `스냅샷이 있으면 기존 결과를 반환한다`() {
        val snapshotUtil = TransferSnapshotUtil(ObjectMapper())
        val snapshotResult = TransferResult(TransferProps.TransferStatusValue.SUCCEEDED, 99L, null)
        val repository = RecordingKeyRepository(
            TestKey(
                status = IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED,
                expiresAt = null,
                responseSnapshot = snapshotUtil.toSnapshot(snapshotResult),
            ),
        )
        val handler = TransferIdempotencyHandler(repository, snapshotUtil)

        val result = handler.resolveExisting(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            null,
        )

        assertThat(result).isEqualTo(snapshotResult)
    }

    @Test
    fun `markFailed는 실패 결과를 스냅샷으로 저장한다`() {
        val repository = RecordingKeyRepository(
            TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS, null, null),
        )
        val snapshotUtil = TransferSnapshotUtil(ObjectMapper())
        val handler = TransferIdempotencyHandler(repository, snapshotUtil)
        val failed = TransferResult.failed(TransferFailureCode.INSUFFICIENT_BALANCE)

        handler.markFailed(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            failed,
            now(),
        )

        val args = requireNotNull(repository.markFailedArgs)
        assertThat(args.memberId).isEqualTo(1L)
        assertThat(args.scope).isEqualTo(IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER)
        assertThat(args.idempotencyKey).isEqualTo("k")
        assertThat(snapshotUtil.fromSnapshot(args.responseSnapshot)).isEqualTo(failed)
    }

    private fun now(): LocalDateTime = LocalDateTime.parse("2026-01-01T10:00:00")

    private data class MarkInProgressArgs(
        val memberId: Long,
        val scope: IdempotencyKeyProps.IdempotencyScopeValue,
        val idempotencyKey: String,
        val requestHash: String,
        val startedAt: LocalDateTime,
    )

    private data class MarkFailedArgs(
        val memberId: Long,
        val scope: IdempotencyKeyProps.IdempotencyScopeValue,
        val idempotencyKey: String,
        val responseSnapshot: String,
        val completedAt: LocalDateTime,
    )

    private class RecordingKeyRepository(
        private val key: IdempotencyKeyModel?,
        private val markInProgressResult: Boolean = false,
    ) : IdempotencyKeyRepository {
        var markInProgressArgs: MarkInProgressArgs? = null
        var markFailedArgs: MarkFailedArgs? = null

        override fun save(props: IdempotencyKeyProps): IdempotencyKeyModel = requireNotNull(key)

        override fun findByKey(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
        ): IdempotencyKeyModel? = key

        override fun tryMarkInProgress(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            requestHash: String,
            startedAt: LocalDateTime,
        ): Boolean {
            markInProgressArgs =
                MarkInProgressArgs(memberId, scope, idempotencyKey, requestHash, startedAt)
            return markInProgressResult
        }

        override fun markSucceeded(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            responseSnapshot: String,
            completedAt: LocalDateTime,
        ): IdempotencyKeyModel = requireNotNull(key)

        override fun markFailed(
            memberId: Long,
            scope: IdempotencyKeyProps.IdempotencyScopeValue,
            idempotencyKey: String,
            responseSnapshot: String,
            completedAt: LocalDateTime,
        ): IdempotencyKeyModel {
            markFailedArgs =
                MarkFailedArgs(memberId, scope, idempotencyKey, responseSnapshot, completedAt)
            return requireNotNull(key)
        }

        override fun markTimeoutBefore(
            cutoff: LocalDateTime,
            responseSnapshot: String,
        ): Int = 0
    }

    private class TestKey(
        override val status: IdempotencyKeyProps.IdempotencyKeyStatusValue,
        override val expiresAt: LocalDateTime?,
        override val responseSnapshot: String?,
    ) : IdempotencyKeyModel {
        override val idempotencyKeyId: Long? = 1L
        override val memberId: Long = 1L
        override val idempotencyKey: String = "k"
        override val scope: IdempotencyKeyProps.IdempotencyScopeValue =
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        override var requestHash: String? = null
        override val startedAt: LocalDateTime? = null
        override val completedAt: LocalDateTime? = null
    }
}
