package org.yechan.remittance.account.repository

import java.time.LocalDateTime
import org.yechan.remittance.account.ProcessedEventRepository

class ProcessedEventRepositoryImpl(
    private val repository: ProcessedEventJpaRepository
) : ProcessedEventRepository {
    override fun existsByEventId(eventId: Long): Boolean {
        return repository.existsByEventId(eventId)
    }

    override fun markProcessed(
        eventId: Long,
        processedAt: LocalDateTime
    ) {
        repository.save(ProcessedEventEntity.create(eventId, processedAt))
    }
}
