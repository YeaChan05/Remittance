package org.yechan.remittance.transfer

import java.time.LocalDateTime

data class OutboxEvent(
    override val eventId: Long?,
    override val aggregateType: String,
    override val aggregateId: String,
    override val eventType: String,
    override val payload: String,
    override val status: OutboxEventProps.OutboxEventStatusValue,
    val createdAt: LocalDateTime?
) : OutboxEventModel
