package org.yechan.remittance.transfer

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate

class TransferEventPublisherImplTest {
    @Test
    fun `이벤트 발행은 헤더와 페이로드를 설정한다`() {
        val rabbitTemplate = mock(RabbitTemplate::class.java)
        val properties = TransferEventPublisherProperties().apply {
            exchange = "transfer.exchange"
            routingKey = "transfer.completed"
        }
        val publisher = TransferEventPublisherImpl(rabbitTemplate, properties)
        val event = OutboxEvent(
            11L,
            "TRANSFER",
            "1",
            "TRANSFER_COMPLETED",
            "payload",
            OutboxEventProps.OutboxEventStatusValue.NEW,
            LocalDateTime.of(2025, 1, 1, 0, 0)
        )

        publisher.publish(event)

        val captor = ArgumentCaptor.forClass(MessagePostProcessor::class.java)
        verify(rabbitTemplate).convertAndSend(
            eq("transfer.exchange"),
            eq("transfer.completed"),
            eq("payload"),
            captor.capture()
        )

        val message = Message("payload".toByteArray(StandardCharsets.UTF_8), MessageProperties())
        captor.value.postProcessMessage(message)

        assertEquals(11L, message.messageProperties.headers["eventId"])
        assertEquals("TRANSFER_COMPLETED", message.messageProperties.headers["eventType"])
    }
}
