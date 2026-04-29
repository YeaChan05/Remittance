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
import org.yechan.remittance.Money
import org.yechan.remittance.transfer.LedgerProps
import org.yechan.remittance.transfer.LedgerRepository
import java.time.LocalDateTime

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransferRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class LedgerRepositoryImplTest @Autowired constructor(
    private val repository: LedgerRepository,
    private val entityManager: EntityManager,
) {
    @Test
    fun `같은 transfer account side ledger는 중복 저장하지 않는다`() {
        val props = TestLedgerProps(1L, 10L, LedgerProps.LedgerSideValue.DEBIT)

        val firstSaved = repository.saveIfAbsent(props)
        val secondSaved = repository.saveIfAbsent(props)
        flushClear()

        assertThat(firstSaved).isTrue()
        assertThat(secondSaved).isFalse()
        assertThat(countLedgers()).isEqualTo(1L)
    }

    private fun flushClear() {
        entityManager.flush()
        entityManager.clear()
    }

    private fun countLedgers(): Long = entityManager
        .createQuery("select count(l) from LedgerEntity l", Long::class.java)
        .singleResult
        .toLong()

    private data class TestLedgerProps(
        override val transferId: Long,
        override val accountId: Long,
        override val side: LedgerProps.LedgerSideValue,
    ) : LedgerProps {
        override val amount: Money = Money.of(100)
        override val createdAt: LocalDateTime = LocalDateTime.parse("2026-01-01T00:00:00")
    }
}
