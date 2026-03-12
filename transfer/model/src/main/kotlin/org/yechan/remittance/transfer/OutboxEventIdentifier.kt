package org.yechan.remittance.transfer

interface OutboxEventIdentifier {
    val eventId: Long?
}
