package org.yechan.remittance.account

import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransferNotificationPayloadParserTest {
    @Test
    fun `payload를 파싱하면 알림 props를 생성한다`() {
        val parser = TransferNotificationPayloadParser()
        val payload =
            """
            {"transferId":11,"fromAccountId":1,"toAccountId":2,"amount":10000,"completedAt":"2025-01-01T00:00"}
            """.trimIndent()

        val props = parser.parse(100L, payload)

        assertEquals(100L, props.eventId)
        assertEquals(11L, props.transferId)
        assertEquals(1L, props.fromAccountId)
        assertEquals(2L, props.toAccountId)
        assertEquals(BigDecimal("10000"), props.amount)
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), props.occurredAt)
    }
}
