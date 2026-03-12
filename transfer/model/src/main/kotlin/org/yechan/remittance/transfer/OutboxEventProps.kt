package org.yechan.remittance.transfer

interface OutboxEventProps {
    val aggregateType: String
    val aggregateId: String
    val eventType: String
    val payload: String
    val status: OutboxEventStatusValue

    enum class OutboxEventStatusValue {
        NEW,
        SENT
    }
}
