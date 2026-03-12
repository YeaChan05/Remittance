package org.yechan.remittance.account.repository

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestConstructor
import org.yechan.remittance.account.AccountProps
import java.math.BigDecimal

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountRepositoryAutoConfiguration::class)
@ContextConfiguration(classes = [TestApplication::class])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AccountJpaRepositoryTest {
    @Autowired
    lateinit var accountRepository: AccountJpaRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun `계좌 엔티티를 저장하면 다시 조회할 수 있다`() {
        val account = AccountEntity.create(TestAccountProps())

        val saved = accountRepository.save(account)
        entityManager.flush()

        assertThat(saved.accountId).isNotNull()
        val byId = accountRepository.findById(saved.accountId!!)
        assertThat(byId).isPresent
        assertThat(byId.get()).isEqualTo(saved)
    }

    private class TestAccountProps : AccountProps {
        override val memberId: Long = 10L
        override val bankCode: String = "090"
        override val accountNumber: String = "123-456"
        override val accountName: String = "sample-account"
        override val balance: BigDecimal = BigDecimal.ZERO
    }
}
