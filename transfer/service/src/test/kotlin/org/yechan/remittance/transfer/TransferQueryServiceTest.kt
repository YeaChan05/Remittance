package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.yechan.remittance.account.AccountIdentifier
import java.math.BigDecimal
import java.time.LocalDateTime

class TransferQueryServiceTest {
    @Test
    fun `계좌 소유자가 맞으면 거래 내역을 조회한다`() {
        val transferRepository = FakeTransferRepository()
        val service = TransferQueryService(
            transferAccountClient = object : TransferAccountClient {
                override fun get(accountId: Long): TransferAccountSnapshot = TransferAccountSnapshot(accountId, 10L, BigDecimal("1000"))

                override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = null

                override fun applyBalanceChange(command: TransferBalanceChangeCommand) = Unit
            },
            transferRepository = transferRepository,
        )

        val result = service.query(10L, 1L, TransferQueryCondition(null, null, null))

        assertThat(result).hasSize(1)
        assertThat(result.first().transferId).isEqualTo(1L)
    }

    @Test
    fun `계좌가 없으면 ACCOUNT_NOT_FOUND를 반환한다`() {
        val service = TransferQueryService(
            transferAccountClient = object : TransferAccountClient {
                override fun get(accountId: Long): TransferAccountSnapshot? = null

                override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = null

                override fun applyBalanceChange(command: TransferBalanceChangeCommand) = Unit
            },
            transferRepository = FakeTransferRepository(),
        )

        assertThatThrownBy { service.query(10L, 1L, TransferQueryCondition(null, null, null)) }
            .isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.ACCOUNT_NOT_FOUND)
    }

    @Test
    fun `계좌 소유자가 다르면 INVALID_REQUEST를 반환한다`() {
        val service = TransferQueryService(
            transferAccountClient = object : TransferAccountClient {
                override fun get(accountId: Long): TransferAccountSnapshot = TransferAccountSnapshot(accountId, 99L, BigDecimal("1000"))

                override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = null

                override fun applyBalanceChange(command: TransferBalanceChangeCommand) = Unit
            },
            transferRepository = FakeTransferRepository(),
        )

        assertThatThrownBy { service.query(10L, 1L, TransferQueryCondition(null, null, null)) }
            .isInstanceOf(TransferFailedException::class.java)
            .extracting("failureCode")
            .isEqualTo(TransferFailureCode.INVALID_REQUEST)
    }

    private class FakeTransferRepository : TransferRepository {
        override fun save(props: TransferRequestProps): TransferModel = throw UnsupportedOperationException()

        override fun findById(identifier: TransferIdentifier): TransferModel? = null

        override fun findCompletedByAccountId(
            identifier: AccountIdentifier,
            condition: TransferQueryCondition,
        ): List<TransferModel> = listOf(
            Transfer(
                transferId = 1L,
                fromAccountId = requireNotNull(identifier.accountId),
                toAccountId = 2L,
                amount = BigDecimal("100"),
                scope = TransferProps.TransferScopeValue.TRANSFER,
                status = TransferProps.TransferStatusValue.SUCCEEDED,
                requestedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
                completedAt = LocalDateTime.of(2026, 1, 1, 0, 1),
            ),
        )

        override fun sumAmountByFromAccountIdAndScopeBetween(
            identifier: AccountIdentifier,
            scope: TransferProps.TransferScopeValue,
            from: LocalDateTime,
            to: LocalDateTime,
        ): BigDecimal = BigDecimal.ZERO
    }
}
