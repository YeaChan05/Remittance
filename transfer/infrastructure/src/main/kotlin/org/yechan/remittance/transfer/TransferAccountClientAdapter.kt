package org.yechan.remittance.transfer

import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse

class TransferAccountClientAdapter(
    private val accountInternalApi: AccountInternalApi,
) : TransferAccountClient {
    override fun get(accountId: Long): TransferAccountSnapshot? = accountInternalApi.get(AccountGetRequest(accountId))?.toSnapshot()

    override fun lock(command: TransferAccountLockCommand): TransferLockedAccounts? = accountInternalApi.lock(
        AccountLockRequest(
            fromAccountId = command.fromAccountId,
            toAccountId = command.toAccountId,
        ),
    )?.toLockedAccounts()

    override fun applyBalanceChange(command: TransferBalanceChangeCommand) {
        accountInternalApi.applyBalanceChange(
            AccountBalanceChangeRequest(
                fromAccountId = command.fromAccountId,
                toAccountId = command.toAccountId,
                fromBalance = command.fromBalance,
                toBalance = command.toBalance,
            ),
        )
    }

    private fun AccountSnapshotResponse.toSnapshot(): TransferAccountSnapshot = TransferAccountSnapshot(
        accountId = accountId,
        memberId = memberId,
        balance = balance,
    )

    private fun AccountLockResponse.toLockedAccounts(): TransferLockedAccounts = TransferLockedAccounts(
        fromAccount = fromAccount.toSnapshot(),
        toAccount = toAccount.toSnapshot(),
    )
}
