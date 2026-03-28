package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.transfer.OutboxEventModel
import org.yechan.remittance.transfer.OutboxEventProps

@Entity
@Table(name = "outbox_events", catalog = "integration")
class OutboxEventEntity() :
    BaseEntity(),
    OutboxEventModel {
    override val eventId: Long?
        get() = id

    @field:Column(nullable = false)
    override var aggregateType: String = ""

    @field:Column(nullable = false)
    override var aggregateId: String = ""

    @field:Column(nullable = false)
    override var eventType: String = ""

    @field:Column(nullable = false)
    override var payload: String = ""

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    override var status: OutboxEventProps.OutboxEventStatusValue =
        OutboxEventProps.OutboxEventStatusValue.NEW

    private constructor(
        aggregateType: String,
        aggregateId: String,
        eventType: String,
        payload: String,
        status: OutboxEventProps.OutboxEventStatusValue,
    ) : this() {
        this.aggregateType = aggregateType
        this.aggregateId = aggregateId
        this.eventType = eventType
        this.payload = payload
        this.status = status
    }

    companion object {
        fun create(props: OutboxEventProps): OutboxEventEntity = OutboxEventEntity(
            props.aggregateType,
            props.aggregateId,
            props.eventType,
            props.payload,
            props.status,
        )
    }
}
