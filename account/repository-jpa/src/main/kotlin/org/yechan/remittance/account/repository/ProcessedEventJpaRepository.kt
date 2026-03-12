package org.yechan.remittance.account.repository

import org.springframework.data.jpa.repository.JpaRepository

interface ProcessedEventJpaRepository : JpaRepository<ProcessedEventEntity, Long> {
    fun existsByEventId(eventId: Long): Boolean
}
