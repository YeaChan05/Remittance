package org.yechan.remittance.account.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.yechan.remittance.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "processed_events", catalog = "integration")
class ProcessedEventEntity protected constructor() : BaseEntity() {
    @field:Column(nullable = false, unique = true)
    var eventId: Long? = null
        protected set

    @field:Column(nullable = false)
    var processedAt: LocalDateTime? = null
        protected set

    private constructor(
        eventId: Long,
        processedAt: LocalDateTime,
    ) : this() {
        this.eventId = eventId
        this.processedAt = processedAt
    }

    companion object {
        fun create(
            eventId: Long,
            processedAt: LocalDateTime,
        ): ProcessedEventEntity = ProcessedEventEntity(eventId, processedAt)
    }
}
