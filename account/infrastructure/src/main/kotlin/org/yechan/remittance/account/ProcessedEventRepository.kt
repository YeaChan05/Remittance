package org.yechan.remittance.account

import java.time.LocalDateTime

interface ProcessedEventRepository {
    fun existsByEventId(eventId: Long): Boolean

    fun markProcessed(
        eventId: Long,
        processedAt: LocalDateTime,
    )
}
