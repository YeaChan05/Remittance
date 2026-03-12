package org.yechan.remittance.transfer

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IdempotencyKeyModelTest {
    @Test
    fun `요청 해시 충돌 여부를 판별한다`() {
        val key: IdempotencyKeyModel = TestKey("hash")

        assertFalse(key.isInvalidRequestHash(null))
        assertFalse(TestKey(null).isInvalidRequestHash("hash"))
        assertTrue(key.isInvalidRequestHash("other"))
        assertFalse(key.isInvalidRequestHash("hash"))
    }

    @Test
    fun `만료 여부를 판별한다`() {
        val now = LocalDateTime.parse("2026-01-01T10:00:00")

        assertFalse(TestKey(null, null).isExpired(now))
        assertFalse(TestKey(null, now.plusMinutes(1)).isExpired(now))
        assertTrue(TestKey(null, now.minusMinutes(1)).isExpired(now))
    }

    private data class TestKey(
        override val requestHash: String?,
        override val expiresAt: LocalDateTime? = LocalDateTime.parse("2026-01-01T09:00:00")
    ) : IdempotencyKeyModel {
        override val idempotencyKeyId: Long? = 1L
        override val memberId: Long = 1L
        override val idempotencyKey: String = "key"
        override val scope: IdempotencyKeyProps.IdempotencyScopeValue =
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        override val status: IdempotencyKeyProps.IdempotencyKeyStatusValue =
            IdempotencyKeyProps.IdempotencyKeyStatusValue.BEFORE_START
        override val responseSnapshot: String? = null
        override val startedAt: LocalDateTime? = null
        override val completedAt: LocalDateTime? = null
    }
}
