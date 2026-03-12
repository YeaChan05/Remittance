package org.yechan.remittance.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TransferNotificationConsumerTest {
    @Test
    fun `알림 이벤트를 consume하면 use case에 위임한다`() {
        val useCase = TestTransferNotificationUseCase()
        val parser = TransferNotificationPayloadParser()
        val consumer = TransferNotificationConsumer(useCase, parser)
        val payload =
            """
            {"transferId":11,"fromAccountId":1,"toAccountId":2,"amount":10000,"completedAt":"2025-01-01T00:00"}
            """.trimIndent()

        consumer.consume(payload, 22L, "TRANSFER_COMPLETED")

        assertNotNull(useCase.lastProps)
        assertEquals(22L, useCase.lastProps!!.eventId)
    }

    @Test
    fun `다른 이벤트 타입은 무시한다`() {
        val useCase = TestTransferNotificationUseCase()
        val parser = TransferNotificationPayloadParser()
        val consumer = TransferNotificationConsumer(useCase, parser)
        val payload =
            """
            {"transferId":11,"fromAccountId":1,"toAccountId":2,"amount":10000,"completedAt":"2025-01-01T00:00"}
            """.trimIndent()

        consumer.consume(payload, 22L, "OTHER_EVENT")

        assertNull(useCase.lastProps)
    }

    @Test
    fun `event id가 없으면 무시한다`() {
        val useCase = TestTransferNotificationUseCase()
        val parser = TransferNotificationPayloadParser()
        val consumer = TransferNotificationConsumer(useCase, parser)
        val payload =
            """
            {"transferId":11,"fromAccountId":1,"toAccountId":2,"amount":10000,"completedAt":"2025-01-01T00:00"}
            """.trimIndent()

        consumer.consume(payload, null, "TRANSFER_COMPLETED")

        assertNull(useCase.lastProps)
    }

    private class TestTransferNotificationUseCase : TransferNotificationUseCase {
        var lastProps: TransferNotificationProps? = null

        override fun notify(props: TransferNotificationProps) {
            lastProps = props
        }
    }
}
