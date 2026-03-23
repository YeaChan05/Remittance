package org.yechan.remittance.transfer

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TransferIdempotencyHandlerTest {
    @Test
    fun `만료된 멱등키 조회는 예외를 던진다`() {
        val now = LocalDateTime.parse("2026-01-01T10:00:00")
        val key = TestKey(
            IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START,
            LocalDateTime.parse("2026-01-01T09:00:00"),
            null,
        )
        val handler = TransferIdempotencyHandler(
            FixedKeyRepository(key),
            TransferSnapshotUtil(ObjectMapper()),
        )

        assertThrows(TransferIdempotencyKeyExpiredException::class.java) {
            handler.loadKey(1L, "k", IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER, now)
        }
    }

    @Test
    fun `요청 해시가 다르면 충돌 예외를 던진다`() {
        val key = TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START, null, null)
        key.requestHash = "hash"
        val handler = TransferIdempotencyHandler(
            FixedKeyRepository(key),
            TransferSnapshotUtil(ObjectMapper()),
        )

        assertThrows(TransferIdempotencyKeyConflictException::class.java) {
            handler.resolveExisting(1L, "k", IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER, "other")
        }
    }

    @Test
    fun `IN_PROGRESS 상태면 진행 중 결과를 반환한다`() {
        val key = TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS, null, null)
        val handler = TransferIdempotencyHandler(
            FixedKeyRepository(key),
            TransferSnapshotUtil(ObjectMapper()),
        )

        val result = handler.resolveExisting(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            null,
        )

        assertEquals(TransferProps.TransferStatusValue.IN_PROGRESS, result.status)
    }

    @Test
    fun `스냅샷이 없으면 진행 중 결과를 반환한다`() {
        val key = TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED, null, null)
        val handler = TransferIdempotencyHandler(
            FixedKeyRepository(key),
            TransferSnapshotUtil(ObjectMapper()),
        )

        val result = handler.resolveExisting(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            null,
        )

        assertEquals(TransferProps.TransferStatusValue.IN_PROGRESS, result.status)
    }

    @Test
    fun `스냅샷이 있으면 기존 결과를 반환한다`() {
        val snapshotUtil = TransferSnapshotUtil(ObjectMapper())
        val snapshotResult = TransferResult(TransferProps.TransferStatusValue.SUCCEEDED, 99L, null)
        val snapshot = snapshotUtil.toSnapshot(snapshotResult)
        val key = TestKey(IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED, null, snapshot)

        val handler = TransferIdempotencyHandler(FixedKeyRepository(key), snapshotUtil)

        val result = handler.resolveExisting(
            1L,
            "k",
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            null,
        )

        assertEquals(snapshotResult.status, result.status)
        assertEquals(snapshotResult.transferId, result.transferId)
    }

    private class FixedKeyRepository(
        private val key: IdempotencyKeyModel?,
    ) : IdempotencyKeyRepository {
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
        ): Boolean = false

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
        ): IdempotencyKeyModel = requireNotNull(key)

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
