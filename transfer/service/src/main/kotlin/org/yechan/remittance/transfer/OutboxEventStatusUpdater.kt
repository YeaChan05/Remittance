package org.yechan.remittance.transfer

import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

open class OutboxEventStatusUpdater(
    private val outboxEventRepository: OutboxEventRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun markSent(identifier: OutboxEventIdentifier) {
        outboxEventRepository.markSent(identifier)
    }
}
