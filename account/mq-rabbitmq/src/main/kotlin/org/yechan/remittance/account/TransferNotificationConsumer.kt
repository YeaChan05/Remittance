package org.yechan.remittance.account

import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.messaging.handler.annotation.Header

class TransferNotificationConsumer(
    private val transferNotificationUseCase: TransferNotificationUseCase,
    private val parser: TransferNotificationPayloadParser
) {
    @RabbitListener(queues = ["\${account.transfer-notification.queue:transfer.completed.queue}"])
    fun consume(
        payload: String,
        @Header(value = "eventId", required = false) eventId: Long?,
        @Header(value = "eventType", required = false) eventType: String?
    ) {
        if (eventId == null || eventType != EVENT_TYPE) {
            return
        }
        val props = parser.parse(eventId, payload)
        transferNotificationUseCase.notify(props)
    }

    private companion object {
        const val EVENT_TYPE = "TRANSFER_COMPLETED"
    }
}
