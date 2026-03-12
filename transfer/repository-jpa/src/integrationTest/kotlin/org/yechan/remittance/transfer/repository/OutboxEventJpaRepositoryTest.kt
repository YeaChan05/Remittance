package org.yechan.remittance.transfer.repository

import jakarta.persistence.EntityManager
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.yechan.remittance.transfer.OutboxEventProps

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransferRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OutboxEventJpaRepositoryTest @Autowired constructor(
    private val repository: OutboxEventJpaRepository,
    private val entityManager: EntityManager
) {
    @Test
    fun `NEW 상태 아웃박스 이벤트만 생성 순으로 조회한다`() {
        val first = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.SENT)
        val third = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        flushClear()

        val results = repository.findNewForPublish(
            OutboxEventProps.OutboxEventStatusValue.NEW,
            null,
            Pageable.unpaged()
        )

        assertThat(results.map { it.eventId }).containsExactly(first.eventId, third.eventId)
    }

    @Test
    fun `조회 limit을 적용한다`() {
        val first = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        flushClear()

        val results = repository.findNewForPublish(
            OutboxEventProps.OutboxEventStatusValue.NEW,
            null,
            PageRequest.of(0, 1)
        )

        assertThat(results.map { it.eventId }).containsExactly(first.eventId)
    }

    @Test
    fun `markSent는 상태를 SENT로 갱신한다`() {
        val event = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        flushClear()

        val updated = repository.markSent(requireNotNull(event.eventId))
        flushClear()

        assertThat(updated).isEqualTo(1)
        val found = repository.findById(requireNotNull(event.eventId))
        assertThat(found).isPresent
        assertThat(found.get().status).isEqualTo(OutboxEventProps.OutboxEventStatusValue.SENT)
    }

    private fun saveOutboxEvent(status: OutboxEventProps.OutboxEventStatusValue): OutboxEventEntity {
        return repository.save(OutboxEventEntity.create(TestOutboxEventProps(status)))
    }

    private fun flushClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private data class TestOutboxEventProps(
        override val status: OutboxEventProps.OutboxEventStatusValue
    ) : OutboxEventProps {
        override val aggregateType: String = "TRANSFER"
        override val aggregateId: String = UUID.randomUUID().toString()
        override val eventType: String = "TRANSFER_COMPLETED"
        override val payload: String = "{\"status\":\"SUCCEEDED\"}"
    }
}
