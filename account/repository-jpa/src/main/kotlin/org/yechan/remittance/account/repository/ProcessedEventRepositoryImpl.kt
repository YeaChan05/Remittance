package org.yechan.remittance.account.repository

import org.yechan.remittance.account.ProcessedEventRepository
import java.time.LocalDateTime

class ProcessedEventRepositoryImpl(
    private val repository: ProcessedEventJpaRepository,
) : ProcessedEventRepository {
    override fun existsByEventId(eventId: Long): Boolean = repository.existsByEventId(eventId)

    override fun markProcessed(
        eventId: Long,
        processedAt: LocalDateTime,
    ) {
        repository.save(ProcessedEventEntity.create(eventId, processedAt))
    }
}
