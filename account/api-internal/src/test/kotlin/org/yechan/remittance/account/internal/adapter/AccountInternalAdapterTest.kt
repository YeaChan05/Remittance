package org.yechan.remittance.account.internal.adapter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.account.AccountInternalBalanceChangeCommand
import org.yechan.remittance.account.AccountInternalLockValue
import org.yechan.remittance.account.AccountInternalQueryUseCase
import org.yechan.remittance.account.AccountInternalSnapshotValue
import org.yechan.remittance.account.AccountInternalUpdateUseCase
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

class AccountInternalAdapterTest {
    @Test
    fun `get은 계좌 조회 유스케이스 결과를 응답으로 변환한다`() {
        val capturedMemberId = AtomicReference<Long>()
        val queryUseCase = object : AccountInternalQueryUseCase {
            override fun get(
                memberId: Long,
                accountId: Long,
            ): AccountInternalSnapshotValue {
                capturedMemberId.set(memberId)
                return AccountInternalSnapshotValue(accountId, 3L, BigDecimal("1000"))
            }

            override fun lock(
                memberId: Long,
                fromAccountId: Long,
                toAccountId: Long,
            ): AccountInternalLockValue? = null
        }
        val adapter =
            AccountInternalAdapter(queryUseCase) { _, _ -> true }

        val response = adapter.get(7L, AccountGetRequest(10L))

        assertThat(capturedMemberId.get()).isEqualTo(7L)
        assertThat(response?.accountId).isEqualTo(10L)
        assertThat(response?.memberId).isEqualTo(3L)
        assertThat(response?.balance).isEqualByComparingTo("1000")
    }

    @Test
    fun `lock은 계좌 잠금 유스케이스 결과를 응답으로 변환한다`() {
        val capturedMemberId = AtomicReference<Long>()
        val queryUseCase = object : AccountInternalQueryUseCase {
            override fun get(
                memberId: Long,
                accountId: Long,
            ): AccountInternalSnapshotValue? = null

            override fun lock(
                memberId: Long,
                fromAccountId: Long,
                toAccountId: Long,
            ): AccountInternalLockValue {
                capturedMemberId.set(memberId)
                return AccountInternalLockValue(
                    AccountInternalSnapshotValue(fromAccountId, 3L, BigDecimal("900")),
                    AccountInternalSnapshotValue(toAccountId, 4L, BigDecimal("100")),
                )
            }
        }
        val adapter =
            AccountInternalAdapter(queryUseCase) { _, _ -> true }

        val response = adapter.lock(7L, AccountLockRequest(10L, 20L))

        assertThat(capturedMemberId.get()).isEqualTo(7L)
        assertThat(response?.fromAccount?.accountId).isEqualTo(10L)
        assertThat(response?.toAccount?.accountId).isEqualTo(20L)
    }

    @Test
    fun `applyBalanceChange는 내부 업데이트 유스케이스에 명령을 위임한다`() {
        val capturedMemberId = AtomicReference<Long>()
        val captured = AtomicReference<AccountInternalBalanceChangeCommand>()
        val updateUseCase = AccountInternalUpdateUseCase { memberId, command ->
            capturedMemberId.set(memberId)
            captured.set(command)
            true
        }
        val queryUseCase = object : AccountInternalQueryUseCase {
            override fun get(
                memberId: Long,
                accountId: Long,
            ): AccountInternalSnapshotValue? = null

            override fun lock(
                memberId: Long,
                fromAccountId: Long,
                toAccountId: Long,
            ): AccountInternalLockValue? = null
        }
        val adapter = AccountInternalAdapter(queryUseCase, updateUseCase)

        val response = adapter.applyBalanceChange(
            7L,
            AccountBalanceChangeRequest(
                fromAccountId = 1L,
                toAccountId = 2L,
                fromBalance = BigDecimal("890"),
                toBalance = BigDecimal("600"),
            ),
        )

        assertThat(response.applied).isTrue()
        assertThat(capturedMemberId.get()).isEqualTo(7L)
        assertThat(captured.get()).isEqualTo(
            AccountInternalBalanceChangeCommand(
                fromAccountId = 1L,
                toAccountId = 2L,
                fromBalance = BigDecimal("890"),
                toBalance = BigDecimal("600"),
            ),
        )
    }
}
