package org.yechan.remittance.transfer

import org.yechan.remittance.Money
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeResponse

class TransferAccountClientAdapter(
    private val accountInternalApi: AccountInternalApi,
) : TransferAccountClient {
    override fun get(
        memberId: Long,
        accountId: Long,
    ): TransferAccountSnapshot? = accountInternalApi.get(memberId, AccountGetRequest(accountId))?.toSnapshot()

    override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = accountInternalApi.lock(
        command.memberId,
        AccountLockRequest(
            fromAccountId = command.fromAccountId,
            toAccountId = command.toAccountId,
        ),
    )?.toLockedAccounts()

    override fun applyBalanceChange(command: TransferBalanceChangeCommand) {
        accountInternalApi.applyBalanceChange(
            command.memberId,
            AccountBalanceChangeRequest(
                fromAccountId = command.fromAccountId,
                toAccountId = command.toAccountId,
                fromBalance = command.fromBalance.amount,
                toBalance = command.toBalance.amount,
            ),
        )
    }

    override fun applyTransferBalanceChange(command: TransferBalanceDeltaCommand): TransferBalanceChangeResult = accountInternalApi.applyTransferBalanceChange(
        command.memberId,
        AccountTransferBalanceChangeRequest(
            fromAccountId = command.fromAccountId,
            toAccountId = command.toAccountId,
            debitAmount = command.debitAmount.amount,
            creditAmount = command.creditAmount.amount,
        ),
    ).toBalanceChangeResult()

    private fun AccountSnapshotResponse.toSnapshot(): TransferAccountSnapshot = TransferAccountSnapshot(
        accountId = accountId,
        memberId = memberId,
        balance = Money.of(balance),
    )

    private fun AccountLockResponse.toLockedAccounts(): TransferLockedAccounts = TransferLockedAccounts(
        fromAccount = fromAccount.toSnapshot(),
        toAccount = toAccount.toSnapshot(),
    )

    private fun AccountTransferBalanceChangeResponse.toBalanceChangeResult(): TransferBalanceChangeResult = TransferBalanceChangeResult(
        status = TransferBalanceChangeStatusValue.valueOf(status),
        fromAccount = fromAccount?.toSnapshot(),
        toAccount = toAccount?.toSnapshot(),
    )
}
