package org.yechan.remittance.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.Money
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeResponse
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeResponse
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference

class TransferAccountClientAdapterTest {
    @Test
    fun `계좌 클라이언트는 provider account internal api를 transfer 전용 타입으로 변환한다`() {
        val capturedMemberIdForGet = AtomicReference<Long>()
        val capturedMemberIdForLock = AtomicReference<Long>()
        val capturedMemberIdForBalanceChange = AtomicReference<Long>()
        val capturedMemberIdForTransferBalanceChange = AtomicReference<Long>()
        val capturedGet = AtomicReference<AccountGetRequest>()
        val capturedLock = AtomicReference<AccountLockRequest>()
        val capturedBalanceChange = AtomicReference<AccountBalanceChangeRequest>()
        val capturedTransferBalanceChange = AtomicReference<AccountTransferBalanceChangeRequest>()
        val accountInternalApi = object : AccountInternalApi {
            override fun get(
                memberId: Long,
                request: AccountGetRequest,
            ): AccountSnapshotResponse {
                capturedMemberIdForGet.set(memberId)
                capturedGet.set(request)
                return AccountSnapshotResponse(request.accountId, 7L, BigDecimal("1000"))
            }

            override fun lock(
                memberId: Long,
                request: AccountLockRequest,
            ): AccountLockResponse {
                capturedMemberIdForLock.set(memberId)
                capturedLock.set(request)
                return AccountLockResponse(
                    AccountSnapshotResponse(request.fromAccountId, 7L, BigDecimal("1000")),
                    AccountSnapshotResponse(request.toAccountId, 8L, BigDecimal("200")),
                )
            }

            override fun applyBalanceChange(
                memberId: Long,
                request: AccountBalanceChangeRequest,
            ): AccountBalanceChangeResponse {
                capturedMemberIdForBalanceChange.set(memberId)
                capturedBalanceChange.set(request)
                return AccountBalanceChangeResponse(true)
            }

            override fun applyTransferBalanceChange(
                memberId: Long,
                request: AccountTransferBalanceChangeRequest,
            ): AccountTransferBalanceChangeResponse {
                capturedMemberIdForTransferBalanceChange.set(memberId)
                capturedTransferBalanceChange.set(request)
                return AccountTransferBalanceChangeResponse(
                    status = "APPLIED",
                    fromAccount = AccountSnapshotResponse(
                        request.fromAccountId,
                        7L,
                        BigDecimal("890"),
                    ),
                    toAccount = AccountSnapshotResponse(request.toAccountId, 8L, BigDecimal("300")),
                )
            }
        }
        val adapter = TransferAccountClientAdapter(accountInternalApi)

        val snapshot = adapter.get(7L, 10L)
        val locked = adapter.lock(TransferAccountLockCommand(7L, 10L, 20L))
        adapter.applyBalanceChange(
            TransferBalanceChangeCommand(
                memberId = 7L,
                fromAccountId = 10L,
                toAccountId = 20L,
                fromBalance = money("890"),
                toBalance = money("300"),
            ),
        )
        val balanceChangeResult = adapter.applyTransferBalanceChange(
            TransferBalanceDeltaCommand(
                memberId = 7L,
                fromAccountId = 10L,
                toAccountId = 20L,
                debitAmount = money("110"),
                creditAmount = money("100"),
            ),
        )

        assertThat(capturedMemberIdForGet.get()).isEqualTo(7L)
        assertThat(capturedGet.get()).isEqualTo(AccountGetRequest(10L))
        assertThat(snapshot).isEqualTo(TransferAccountSnapshot(10L, 7L, money("1000")))
        assertThat(capturedMemberIdForLock.get()).isEqualTo(7L)
        assertThat(capturedLock.get()).isEqualTo(AccountLockRequest(10L, 20L))
        assertThat(locked?.fromAccount?.accountId).isEqualTo(10L)
        assertThat(locked?.toAccount?.memberId).isEqualTo(8L)
        assertThat(capturedMemberIdForBalanceChange.get()).isEqualTo(7L)
        assertThat(capturedBalanceChange.get()).isEqualTo(
            AccountBalanceChangeRequest(
                fromAccountId = 10L,
                toAccountId = 20L,
                fromBalance = BigDecimal("890.00"),
                toBalance = BigDecimal("300.00"),
            ),
        )
        assertThat(capturedMemberIdForTransferBalanceChange.get()).isEqualTo(7L)
        assertThat(capturedTransferBalanceChange.get()).isEqualTo(
            AccountTransferBalanceChangeRequest(
                fromAccountId = 10L,
                toAccountId = 20L,
                debitAmount = BigDecimal("110.00"),
                creditAmount = BigDecimal("100.00"),
            ),
        )
        assertThat(balanceChangeResult.status).isEqualTo(TransferBalanceChangeStatusValue.APPLIED)
        assertThat(balanceChangeResult.fromAccount?.balance).isEqualTo(money("890"))
    }

    private fun money(value: String): Money = Money.of(BigDecimal(value))
}
