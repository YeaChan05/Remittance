package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging

fun interface TransferEventPublishUseCase {
    fun publish(limit: Int?): Int
}

class TransferEventPublishService(
    private val outboxEventRepository: OutboxEventRepository,
    private val transferEventPublisher: TransferEventPublisher
) : TransferEventPublishUseCase {
    private val log = KotlinLogging.logger {}

    override fun publish(limit: Int?): Int {
        log.info { "transfer.event.publish.start limit=$limit" }
        val events = outboxEventRepository.findNewForPublish(limit)
        var published = 0
        for (event in events) {
            try {
                log.debug { "transfer.event.publish.try eventId=${event.eventId}" }
                transferEventPublisher.publish(event)
                outboxEventRepository.markSent(event)
                published += 1
                log.info { "transfer.event.publish.success eventId=${event.eventId}" }
            } catch (ex: RuntimeException) {
                log.error(ex) { "transfer.event.publish.failed eventId=${event.eventId}" }
                break
            }
        }
        log.info { "transfer.event.publish.done published=$published" }
        return published
    }
}
