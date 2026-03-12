package org.yechan.remittance.account.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.yechan.remittance.BaseEntity

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
        processedAt: LocalDateTime
    ) : this() {
        this.eventId = eventId
        this.processedAt = processedAt
    }

    companion object {
        fun create(
            eventId: Long,
            processedAt: LocalDateTime
        ): ProcessedEventEntity {
            return ProcessedEventEntity(eventId, processedAt)
        }
    }
}
