package org.yechan.remittance.transfer.repository

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.yechan.remittance.transfer.IdempotencyKeyModel
import org.yechan.remittance.transfer.IdempotencyKeyProps
import org.yechan.remittance.transfer.IdempotencyKeyRepository
import java.time.Duration
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransferRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class IdempotencyKeyJpaRepositoryTest @Autowired constructor(
    private val repository: IdempotencyKeyRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `BEFORE_START 상태일 때만 IN_PROGRESS로 갱신한다`() {
        val now = LocalDateTime.parse("2026-01-01T00:00:00")
        val saved = saveIdempotencyKey(now, 10L, "idem-key")
        flushClear()

        val updated = repository.tryMarkInProgress(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "hash",
            now,
        )
        flushClear()

        assertThat(updated).isTrue()
        val found = repository.findByKey(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
        )
        assertThat(found).isNotNull()
        assertThat(found?.status).isEqualTo(IdempotencyKeyProps.IdempotencyKeyStatusValue.IN_PROGRESS)
        assertThat(found?.requestHash).isEqualTo("hash")
        assertThat(found?.startedAt).isEqualTo(now)

        val secondUpdate = repository.tryMarkInProgress(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "hash-2",
            now.plusSeconds(30),
        )
        assertThat(secondUpdate).isFalse()
    }

    @Test
    fun `성공 처리 시 응답 스냅샷과 완료 시간을 저장한다`() {
        val now = LocalDateTime.parse("2026-01-02T00:00:00")
        val saved = saveIdempotencyKey(now, 20L, "idem-succeed")
        flushClear()

        repository.tryMarkInProgress(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "hash",
            now,
        )
        repository.markSucceeded(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "{\"status\":\"SUCCEEDED\"}",
            now.plusSeconds(30),
        )
        flushClear()

        val found = repository.findByKey(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
        )
        assertThat(found).isNotNull()
        assertThat(found?.status).isEqualTo(IdempotencyKeyProps.IdempotencyKeyStatusValue.SUCCEEDED)
        assertThat(found?.responseSnapshot).contains("SUCCEEDED")
        assertThat(found?.completedAt).isEqualTo(now.plusSeconds(30))
    }

    @Test
    fun `실패 처리 시 응답 스냅샷과 완료 시간을 저장한다`() {
        val now = LocalDateTime.parse("2026-01-03T00:00:00")
        val saved = saveIdempotencyKey(now, 30L, "idem-failed")
        flushClear()

        repository.tryMarkInProgress(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "hash",
            now,
        )
        repository.markFailed(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "{\"status\":\"FAILED\"}",
            now.plusSeconds(10),
        )
        flushClear()

        val found = repository.findByKey(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
        )
        assertThat(found).isNotNull()
        assertThat(found?.status).isEqualTo(IdempotencyKeyProps.IdempotencyKeyStatusValue.FAILED)
        assertThat(found?.responseSnapshot).contains("FAILED")
        assertThat(found?.completedAt).isEqualTo(now.plusSeconds(10))
    }

    @Test
    fun `오래된 IN_PROGRESS 건은 TIMEOUT으로 전환한다`() {
        val now = LocalDateTime.parse("2026-01-04T00:00:00")
        val saved = saveIdempotencyKey(now, 40L, "idem-timeout")
        flushClear()

        repository.tryMarkInProgress(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
            "hash",
            now.minus(Duration.ofMinutes(10)),
        )
        flushClear()

        val updated = repository.markTimeoutBefore(
            now.minus(Duration.ofMinutes(5)),
            "{\"status\":\"FAILED\",\"error_code\":\"TIMEOUT\"}",
        )
        flushClear()

        assertThat(updated).isEqualTo(1)
        val found = repository.findByKey(
            saved.memberId,
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER,
            saved.idempotencyKey,
        )
        assertThat(found).isNotNull()
        assertThat(found?.status).isEqualTo(IdempotencyKeyProps.IdempotencyKeyStatusValue.TIMEOUT)
        assertThat(found?.responseSnapshot).contains("TIMEOUT")
    }

    private fun flushClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun saveIdempotencyKey(
        now: LocalDateTime,
        memberId: Long,
        idempotencyKey: String,
    ): IdempotencyKeyModel = repository.save(TestIdempotencyKeyProps(memberId, idempotencyKey, now))

    private data class TestIdempotencyKeyProps(
        override val memberId: Long,
        override val idempotencyKey: String,
        private val now: LocalDateTime,
    ) : IdempotencyKeyProps {
        override val expiresAt: LocalDateTime
            get() = now.plus(Duration.ofMinutes(20))
        override val scope: IdempotencyKeyProps.IdempotencyScopeValue =
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        override val status: IdempotencyKeyProps.IdempotencyKeyStatusValue? = null
        override val requestHash: String? = null
        override val responseSnapshot: String? = null
        override val startedAt: LocalDateTime? = null
        override val completedAt: LocalDateTime? = null
    }
}
