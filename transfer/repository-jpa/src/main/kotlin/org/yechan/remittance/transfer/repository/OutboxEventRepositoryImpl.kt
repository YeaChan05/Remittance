package org.yechan.remittance.transfer.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.yechan.remittance.transfer.OutboxEventIdentifier
import org.yechan.remittance.transfer.OutboxEventModel
import org.yechan.remittance.transfer.OutboxEventProps
import org.yechan.remittance.transfer.OutboxEventRepository

class OutboxEventRepositoryImpl(
    private val repository: OutboxEventJpaRepository
) : OutboxEventRepository {
    override fun save(props: OutboxEventProps): OutboxEventModel {
        return repository.save(OutboxEventEntity.create(props))
    }

    override fun findNewForPublish(limit: Int?): List<OutboxEventModel> {
        val pageable = if (limit == null) Pageable.unpaged() else PageRequest.of(0, limit)
        return repository.findNewForPublish(OutboxEventProps.OutboxEventStatusValue.NEW, null, pageable)
            .map { it as OutboxEventModel }
    }

    override fun markSent(identifier: OutboxEventIdentifier) {
        repository.markSent(requireNotNull(identifier.eventId))
    }
}
