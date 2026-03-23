package org.yechan.remittance.transfer

import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.core.RabbitTemplate

class TransferEventPublisherImpl(
    private val rabbitTemplate: RabbitTemplate,
    private val properties: TransferEventPublisherProperties,
) : TransferEventPublisher {
    override fun publish(event: OutboxEventModel) {
        val processor = MessagePostProcessor { message ->
            message.messageProperties.setHeader("eventId", requireNotNull(event.eventId))
            message.messageProperties.setHeader("eventType", event.eventType)
            message
        }

        rabbitTemplate.convertAndSend(
            properties.exchange,
            properties.routingKey,
            event.payload,
            processor,
        )
    }
}
