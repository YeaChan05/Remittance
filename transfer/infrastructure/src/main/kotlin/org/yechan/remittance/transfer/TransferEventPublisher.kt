package org.yechan.remittance.transfer

fun interface TransferEventPublisher {
    fun publish(event: OutboxEventModel)
}
