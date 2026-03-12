package org.yechan.remittance.transfer

interface OutboxEventRepository {
    fun save(props: OutboxEventProps): OutboxEventModel

    fun findNewForPublish(limit: Int?): List<OutboxEventModel>

    fun markSent(identifier: OutboxEventIdentifier)
}
