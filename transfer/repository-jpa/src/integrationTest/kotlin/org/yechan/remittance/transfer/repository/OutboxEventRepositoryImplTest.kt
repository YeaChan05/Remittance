package org.yechan.remittance.transfer.repository

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.yechan.remittance.transfer.OutboxEventIdentifier
import org.yechan.remittance.transfer.OutboxEventModel
import org.yechan.remittance.transfer.OutboxEventProps
import org.yechan.remittance.transfer.OutboxEventRepository
import java.util.UUID

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransferRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class OutboxEventRepositoryImplTest @Autowired constructor(
    private val repository: OutboxEventRepository,
    private val entityManager: EntityManager,
) {
    @BeforeEach
    fun setUp() {
        clearOutboxEvents()
    }

    @Test
    fun `save는 props를 entity로 변환해 저장한다`() {
        val saved = repository.save(TestOutboxEventProps(OutboxEventProps.OutboxEventStatusValue.NEW))
        flushClear()

        val found = entityManager.find(OutboxEventEntity::class.java, requireNotNull(saved.eventId))

        assertThat(found).isNotNull
        assertThat(found.aggregateType).isEqualTo("TRANSFER")
        assertThat(found.eventType).isEqualTo("TRANSFER_COMPLETED")
        assertThat(found.status).isEqualTo(OutboxEventProps.OutboxEventStatusValue.NEW)
    }

    @Test
    fun `findNewForPublish는 limit이 없으면 NEW 상태만 생성 순으로 조회한다`() {
        val first = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.SENT)
        val third = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        flushClear()

        val results = repository.findNewForPublish(null)

        assertThat(results.map { it.eventId }).containsExactly(first.eventId, third.eventId)
    }

    @Test
    fun `findNewForPublish는 limit을 적용한다`() {
        val first = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        flushClear()

        val results = repository.findNewForPublish(1)

        assertThat(results.map { it.eventId }).containsExactly(first.eventId)
    }

    @Test
    fun `markSent는 상태를 SENT로 갱신한다`() {
        val event = saveOutboxEvent(OutboxEventProps.OutboxEventStatusValue.NEW)
        flushClear()

        repository.markSent(TestOutboxEventIdentifier(requireNotNull(event.eventId)))
        flushClear()

        val found = entityManager.find(OutboxEventEntity::class.java, requireNotNull(event.eventId))
        assertThat(found.status).isEqualTo(OutboxEventProps.OutboxEventStatusValue.SENT)
    }

    @Test
    fun `markSent는 event id가 없으면 예외를 던진다`() {
        assertThatThrownBy {
            repository.markSent(TestOutboxEventIdentifier(null))
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Required value was null")
    }

    private fun saveOutboxEvent(status: OutboxEventProps.OutboxEventStatusValue): OutboxEventModel = repository.save(TestOutboxEventProps(status))

    private fun flushClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun clearOutboxEvents() {
        entityManager.createQuery("delete from OutboxEventEntity").executeUpdate()
        flushClear()
    }

    private data class TestOutboxEventProps(
        override val status: OutboxEventProps.OutboxEventStatusValue,
    ) : OutboxEventProps {
        override val aggregateType: String = "TRANSFER"
        override val aggregateId: String = UUID.randomUUID().toString()
        override val eventType: String = "TRANSFER_COMPLETED"
        override val payload: String = """{"status":"SUCCEEDED"}"""
    }

    private data class TestOutboxEventIdentifier(
        override val eventId: Long?,
    ) : OutboxEventIdentifier
}
