package org.yechan.remittance.transfer

import org.springframework.scheduling.annotation.Scheduled
import org.yechan.remittance.SuppressP6SpySqlLog

open class TransferOutboxPublisher(
    private val transferEventPublishUseCase: TransferEventPublishUseCase,
    private val properties: TransferOutboxProperties,
) {
    @SuppressP6SpySqlLog
    @Scheduled(fixedDelayString = "\${transfer.outbox.publish-delay-ms:1000}")
    open fun publish() {
        transferEventPublishUseCase.publish(properties.batchSize)
    }
}
