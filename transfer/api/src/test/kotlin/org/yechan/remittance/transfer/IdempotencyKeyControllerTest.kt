package org.yechan.remittance.transfer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class IdempotencyKeyControllerTest {
    @Test
    fun `멱등키 생성 요청은 응답을 반환한다`() {
        val expiresAt = LocalDateTime.parse("2026-01-01T01:00:00")
        val useCase = IdempotencyKeyCreateUseCase { TestIdempotencyKeyModel(expiresAt) }

        val controller = IdempotencyKeyController(useCase)

        val response = controller.create(1L, null)

        assertNotNull(response.body)
        assertEquals("key", response.body?.idempotencyKey)
        assertEquals(expiresAt, response.body?.expiresAt)
    }

    private data class TestIdempotencyKeyModel(
        override val expiresAt: LocalDateTime,
    ) : IdempotencyKeyModel {
        override val idempotencyKeyId: Long? = 1L
        override val memberId: Long = 1L
        override val idempotencyKey: String = "key"
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
