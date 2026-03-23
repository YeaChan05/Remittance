package org.yechan.remittance.transfer

import org.springframework.scheduling.annotation.Scheduled

class TransferOutboxPublisher(
    private val transferEventPublishUseCase: TransferEventPublishUseCase,
    private val properties: TransferOutboxProperties,
) {
    @Scheduled(fixedDelayString = "\${transfer.outbox.publish-delay-ms:1000}")
    fun publish() {
        transferEventPublishUseCase.publish(properties.batchSize)
    }
}
