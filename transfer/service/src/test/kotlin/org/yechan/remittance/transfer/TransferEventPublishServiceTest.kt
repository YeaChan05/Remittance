package org.yechan.remittance.transfer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TransferEventPublishServiceTest {
    @Test
    fun `신규 아웃박스 이벤트를 발행하면 SENT로 마킹한다`() {
        val repository = FakeOutboxEventRepository(sampleEvents())
        val publisher = FakeTransferEventPublisher()
        val updater = FakeOutboxEventStatusUpdater(repository)
        val service = TransferEventPublishService(repository, publisher, updater)

        val published = service.publish(10)

        assertEquals(2, published)
        assertEquals(listOf(1L, 2L), repository.sentEventIds)
        assertEquals(2, publisher.publishedCount())
    }

    @Test
    fun `발행 중 예외가 나면 즉시 중단한다`() {
        val repository = FakeOutboxEventRepository(sampleEvents())
        val publisher = FakeTransferEventPublisher().apply { failOn(2L) }
        val updater = FakeOutboxEventStatusUpdater(repository)
        val service = TransferEventPublishService(repository, publisher, updater)

        val published = service.publish(10)

        assertEquals(1, published)
        assertEquals(listOf(1L), repository.sentEventIds)
        assertEquals(1, publisher.publishedCount())
    }

    private fun sampleEvents(): List<OutboxEvent> = listOf(
        OutboxEvent(1L, "TRANSFER", "1", "TRANSFER_COMPLETED", "payload", status(), now()),
        OutboxEvent(2L, "TRANSFER", "2", "TRANSFER_COMPLETED", "payload", status(), now()),
    )

    private fun status(): OutboxEventProps.OutboxEventStatusValue = OutboxEventProps.OutboxEventStatusValue.NEW

    private fun now(): LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0)

    private class FakeOutboxEventRepository(
        private val events: List<OutboxEvent>,
    ) : OutboxEventRepository {
        val sentEventIds = mutableListOf<Long>()

        override fun save(props: OutboxEventProps): OutboxEventModel = throw UnsupportedOperationException("Not needed")

        override fun findNewForPublish(limit: Int?): List<OutboxEventModel> = events

        override fun markSent(identifier: OutboxEventIdentifier) {
            sentEventIds += requireNotNull(identifier.eventId)
        }
    }

    private class FakeTransferEventPublisher : TransferEventPublisher {
        private val published = mutableListOf<Long>()
        private var failOnEventId: Long? = null

        override fun publish(event: OutboxEventModel) {
            if (event.eventId == failOnEventId) {
                throw IllegalStateException("publish failed")
            }
            published += requireNotNull(event.eventId)
        }

        fun failOn(eventId: Long) {
            failOnEventId = eventId
        }

        fun publishedCount(): Int = published.size
    }

    private class FakeOutboxEventStatusUpdater(
        private val repository: OutboxEventRepository,
    ) : OutboxEventStatusUpdater(repository) {
        override fun markSent(identifier: OutboxEventIdentifier) {
            repository.markSent(identifier)
        }
    }
}
