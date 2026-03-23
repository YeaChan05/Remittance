package org.yechan.remittance.transfer.repository

import jakarta.persistence.EntityManager
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
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransferRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class TransferJpaRepositoryTest @Autowired constructor(
    private val repository: TransferJpaRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `완료된 이체만 계좌 기준으로 최신순 조회한다`() {
        val now = LocalDateTime.parse("2026-02-01T00:00:00")
        saveTransfer(1L, 2L, TransferProps.TransferStatusValue.SUCCEEDED, now.minusSeconds(120))
        saveTransfer(3L, 1L, TransferProps.TransferStatusValue.FAILED, now.minusSeconds(30))
        saveTransfer(1L, 4L, TransferProps.TransferStatusValue.IN_PROGRESS, null)
        saveTransfer(5L, 6L, TransferProps.TransferStatusValue.SUCCEEDED, now.minusSeconds(10))
        flushClear()

        val results = repository.findCompletedByAccountId(
            1L,
            COMPLETED_STATUSES,
            now.minusSeconds(300),
            now,
            Pageable.unpaged(),
        )

        assertThat(results).hasSize(2)
        assertThat(results.map { it.completedAt }).containsExactly(now.minusSeconds(30), now.minusSeconds(120))
        assertThat(results[0].status).isEqualTo(TransferProps.TransferStatusValue.FAILED)
        assertThat(results[1].status).isEqualTo(TransferProps.TransferStatusValue.SUCCEEDED)
    }

    @Test
    fun `조회 기간과 limit을 함께 적용한다`() {
        val now = LocalDateTime.parse("2026-02-02T00:00:00")
        saveTransfer(1L, 2L, TransferProps.TransferStatusValue.SUCCEEDED, now.minusSeconds(100))
        saveTransfer(1L, 3L, TransferProps.TransferStatusValue.SUCCEEDED, now.minusSeconds(50))
        saveTransfer(1L, 4L, TransferProps.TransferStatusValue.SUCCEEDED, now.minusSeconds(10))
        flushClear()

        val results = repository.findCompletedByAccountId(
            1L,
            COMPLETED_STATUSES,
            now.minusSeconds(60),
            now,
            PageRequest.of(0, 1),
        )

        assertThat(results).hasSize(1)
        assertThat(results[0].completedAt).isEqualTo(now.minusSeconds(10))
    }

    private fun saveTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        status: TransferProps.TransferStatusValue,
        completedAt: LocalDateTime?,
    ) {
        repository.save(
            TransferEntity.create(
                TestTransferProps(
                    fromAccountId,
                    toAccountId,
                    status,
                    completedAt ?: LocalDateTime.parse("2026-02-01T00:00:00"),
                    completedAt,
                ),
            ),
        )
    }

    private fun flushClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private data class TestTransferProps(
        override val fromAccountId: Long,
        override val toAccountId: Long,
        override val status: TransferProps.TransferStatusValue,
        override val requestedAt: LocalDateTime,
        override val completedAt: LocalDateTime?,
    ) : TransferProps {
        override val amount: BigDecimal = BigDecimal.valueOf(1000L)
        override val scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.DEPOSIT
    }

    private companion object {
        val COMPLETED_STATUSES = listOf(
            TransferProps.TransferStatusValue.SUCCEEDED,
            TransferProps.TransferStatusValue.FAILED,
        )
    }
}
