package org.yechan.remittance.account

import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AccountDeleteServiceTest {
    @Test
    fun `계좌 소유자가 일치하면 계좌를 삭제한다`() {
        val deletedId = AtomicReference<Long>()
        val repository: AccountRepository =
            TestAccountRepository(
                TestAccount(1L, 10L),
                deletedId
            )
        val useCase: AccountDeleteUseCase = AccountDeleteService(repository)

        val deleted = useCase.delete(TestDeleteProps(1L, 10L))

        assertThat(deleted.accountId).isEqualTo(10L)
        assertThat(deletedId.get()).isEqualTo(10L)
    }

    @Test
    fun `계좌 소유자가 다르면 예외를 던진다`() {
        val repository: AccountRepository =
            TestAccountRepository(
                TestAccount(2L, 10L),
                AtomicReference()
            )
        val useCase: AccountDeleteUseCase = AccountDeleteService(repository)

        assertThatThrownBy { useCase.delete(TestDeleteProps(1L, 10L)) }
            .isInstanceOf(AccountPermissionDeniedException::class.java)
            .hasMessage("Account owner mismatch")
    }

    @Test
    fun `계좌가 없으면 예외를 던진다`() {
        val repository: AccountRepository =
            TestAccountRepository(
                null,
                AtomicReference()
            )
        val useCase: AccountDeleteUseCase = AccountDeleteService(repository)

        assertThatThrownBy { useCase.delete(TestDeleteProps(1L, 10L)) }
            .isInstanceOf(AccountNotFoundException::class.java)
            .hasMessage("Account not found")
    }

    private class TestAccountRepository(
        private val account: AccountModel?,
        private val deletedId: AtomicReference<Long>
    ) : AccountRepository {
        override fun save(props: AccountProps): AccountModel {
            throw UnsupportedOperationException()
        }

        override fun findById(identifier: AccountIdentifier): AccountModel? {
            return account
        }

        override fun findByIdForUpdate(identifier: AccountIdentifier): AccountModel? {
            return null
        }

        override fun findByMemberIdAndBankCodeAndAccountNumber(
            memberId: Long?,
            bankCode: String,
            accountNumber: String
        ): AccountModel? {
            return null
        }

        override fun delete(identifier: AccountIdentifier) {
            deletedId.set(identifier.accountId)
        }
    }

    private data class TestDeleteProps(
        override val memberId: Long,
        override val accountId: Long
    ) : AccountDeleteProps

    private data class TestAccount(
        override val memberId: Long?,
        override val accountId: Long?
    ) : AccountModel {
        override val bankCode: String = "001"
        override val accountNumber: String = "123"
        override val accountName: String = "name"
        override val balance: BigDecimal = BigDecimal.ZERO
    }
}
