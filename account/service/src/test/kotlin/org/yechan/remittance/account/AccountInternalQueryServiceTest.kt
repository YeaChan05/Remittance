package org.yechan.remittance.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class AccountInternalQueryServiceTest {
    @Test
    fun `계좌 조회는 최소 스냅샷을 반환한다`() {
        val repository = TestAccountRepository(
            accounts = mapOf(1L to TestAccount(1L, 10L, BigDecimal("1000"))),
        )
        val useCase = AccountInternalQueryService(repository)

        val result = useCase.get(10L, 1L)

        assertThat(result).isEqualTo(AccountInternalSnapshotValue(1L, 10L, BigDecimal("1000")))
    }

    @Test
    fun `계좌 잠금은 작은 계좌 ID부터 잠근 뒤 원래 순서로 반환한다`() {
        val repository = TestAccountRepository(
            accounts = mapOf(
                1L to TestAccount(1L, 10L, BigDecimal("1000")),
                2L to TestAccount(2L, 20L, BigDecimal("2000")),
            ),
        )
        val useCase = AccountInternalQueryService(repository)

        val result = useCase.lock(10L, 2L, 1L)

        assertThat(repository.lockedIds).containsExactly(1L, 2L)
        assertThat(result?.fromAccount?.accountId).isEqualTo(2L)
        assertThat(result?.toAccount?.accountId).isEqualTo(1L)
    }

    @Test
    fun `같은 계좌 잠금은 한 번만 조회해 양쪽에 같은 스냅샷을 반환한다`() {
        val repository = TestAccountRepository(
            accounts = mapOf(1L to TestAccount(1L, 10L, BigDecimal("1000"))),
        )
        val useCase = AccountInternalQueryService(repository)

        val result = useCase.lock(10L, 1L, 1L)

        assertThat(repository.lockedIds).containsExactly(1L)
        assertThat(result?.fromAccount).isEqualTo(result?.toAccount)
    }

    private class TestAccountRepository(
        private val accounts: Map<Long, TestAccount>,
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
        ): AccountModel? = throw UnsupportedOperationException()

        override fun delete(identifier: AccountIdentifier) = throw UnsupportedOperationException()
    }

    private data class TestAccount(
        override val accountId: Long?,
        override val memberId: Long?,
        override var balance: BigDecimal,
    ) : AccountModel {
        override val bankCode: String = "001"
        override val accountNumber: String = "123"
        override val accountName: String = "name"

        override fun updateBalance(balance: BigDecimal) {
            this.balance = balance
        }
    }
}
