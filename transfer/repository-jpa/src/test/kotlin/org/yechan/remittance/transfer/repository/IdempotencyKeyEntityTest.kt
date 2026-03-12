package org.yechan.remittance.transfer.repository

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yechan.remittance.transfer.IdempotencyKeyProps

class IdempotencyKeyEntityTest {
    @Test
    fun `생성 시 기본 상태를 초기화한다`() {
        val entity = IdempotencyKeyEntity.create(TestProps())

        assertEquals(IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START, entity.status)
        assertNull(entity.requestHash)
        assertNull(entity.responseSnapshot)
    }

    @Test
    fun `IN_PROGRESS 전환은 한 번만 성공한다`() {
        val entity = IdempotencyKeyEntity.create(TestProps())
        val startedAt = LocalDateTime.parse("2026-01-01T10:00:00")

        assertTrue(entity.tryMarkInProgress("hash", startedAt))
        assertEquals(IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS, entity.status)
        assertEquals("hash", entity.requestHash)
        assertEquals(startedAt, entity.startedAt)
        assertFalse(entity.tryMarkInProgress("hash2", startedAt))
    }

    @Test
    fun `TIMEOUT 전환은 진행 중이고 시작 시간이 cutoff 이전일 때만 가능하다`() {
        val entity = IdempotencyKeyEntity.create(TestProps())
        val cutoff = LocalDateTime.parse("2026-01-01T10:00:00")

        entity.markSucceeded("snapshot", cutoff)
        assertFalse(entity.markTimeoutIfBefore(cutoff, "timeout", cutoff))

        val inProgressNoStart = IdempotencyKeyEntity.create(TestProps())
        inProgressNoStart.tryMarkInProgress("hash", null)
        assertFalse(inProgressNoStart.markTimeoutIfBefore(cutoff, "timeout", cutoff))

        val inProgressAfterCutoff = IdempotencyKeyEntity.create(TestProps())
        inProgressAfterCutoff.tryMarkInProgress("hash", cutoff)
        assertFalse(inProgressAfterCutoff.markTimeoutIfBefore(cutoff, "timeout", cutoff))
    }

    @Test
    fun `TIMEOUT 전환은 상태와 응답 스냅샷을 갱신한다`() {
        val entity = IdempotencyKeyEntity.create(TestProps())
        val startedAt = LocalDateTime.parse("2026-01-01T09:00:00")
        val cutoff = LocalDateTime.parse("2026-01-01T10:00:00")
        val completedAt = LocalDateTime.parse("2026-01-01T10:30:00")

        entity.tryMarkInProgress("hash", startedAt)

        assertTrue(entity.markTimeoutIfBefore(cutoff, "timeout", completedAt))
        assertEquals(IdempotencyKeyProps.IdempotencyKeyStatusValue.TIMEOUT, entity.status)
        assertEquals("timeout", entity.responseSnapshot)
        assertEquals(completedAt, entity.completedAt)
    }

    private class TestProps : IdempotencyKeyProps {
        override val memberId: Long = 1L
        override val idempotencyKey: String = "key"
        override val expiresAt: LocalDateTime = LocalDateTime.parse("2026-01-02T00:00:00")
        override val scope: IdempotencyKeyProps.IdempotencyScopeValue =
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        override val status: IdempotencyKeyProps.IdempotencyKeyStatusValue =
            IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START
        override val requestHash: String? = null
        override val responseSnapshot: String? = null
        override val startedAt: LocalDateTime? = null
        override val completedAt: LocalDateTime? = null
    }
}
