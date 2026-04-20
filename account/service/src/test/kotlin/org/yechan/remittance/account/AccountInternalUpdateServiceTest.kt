package org.yechan.remittance.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.Money
import java.math.BigDecimal

class AccountInternalUpdateServiceTest {
    @Test
    fun `서로 다른 계좌 잔액을 갱신한다`() {
        val accounts = mutableMapOf(
            1L to TestAccount(1L, 10L, money("1000")),
            2L to TestAccount(2L, 20L, money("500")),
        )
        val repository = TestAccountRepository(accounts)
        val useCase = AccountInternalUpdateService(repository)

        val applied = useCase.applyBalanceChange(
            10L,
            AccountInternalBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 2L,
                fromBalance = money("890"),
                toBalance = money("600"),
            ),
        )

        assertThat(applied).isTrue()
        assertThat(accounts.getValue(1L).balance).isEqualTo(money("890"))
        assertThat(accounts.getValue(2L).balance).isEqualTo(money("600"))
    }

    @Test
    fun `같은 계좌 잔액 갱신은 한 번만 반영한다`() {
        val accounts = mutableMapOf(
            1L to TestAccount(1L, 10L, money("1000")),
        )
        val repository = TestAccountRepository(accounts)
        val useCase = AccountInternalUpdateService(repository)

        val applied = useCase.applyBalanceChange(
            10L,
            AccountInternalBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 1L,
                fromBalance = money("1200"),
                toBalance = money("1200"),
            ),
        )

        assertThat(applied).isTrue()
        assertThat(accounts.getValue(1L).balance).isEqualTo(money("1200"))
        assertThat(repository.lockedIds).containsExactly(1L)
    }

    private class TestAccountRepository(
        private val accounts: MutableMap<Long, TestAccount>,
    ) : AccountRepository {
        val lockedIds = mutableListOf<Long>()

        override fun save(props: AccountProps): AccountModel = throw UnsupportedOperationException()

        override fun findById(identifier: AccountIdentifier): AccountModel? = accounts[identifier.accountId]

        override fun findByIdForUpdate(identifier: AccountIdentifier): AccountModel? {
            lockedIds += requireNotNull(identifier.accountId)
            return accounts[identifier.accountId]
        }

        override fun findByMemberIdAndBankCodeAndAccountNumber(
            memberId: Long?,
            bankCode: String,
            accountNumber: String,
        ): AccountModel = throw UnsupportedOperationException()

        override fun delete(identifier: AccountIdentifier) = throw UnsupportedOperationException()
    }

    private data class TestAccount(
        override val accountId: Long?,
        override val memberId: Long?,
        override var balance: Money,
    ) : AccountModel {
        override val bankCode: String = "001"
        override val accountNumber: String = "123"
        override val accountName: String = "name"

        override fun updateBalance(balance: Money) {
            this.balance = balance
        }
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value))
}
