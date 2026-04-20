package org.yechan.remittance.transfer

import org.yechan.remittance.Money

interface TransferAccountClient {
    fun get(
        memberId: Long,
        accountId: Long,
    ): TransferAccountSnapshot?

    fun lock(command: TransferAccountLockCommand): TransferLockedAccounts?

    fun applyBalanceChange(command: TransferBalanceChangeCommand)
}

data class TransferAccountSnapshot(
    val accountId: Long,
    val memberId: Long,
    val balance: Money,
)

data class TransferAccountLockCommand(
    val memberId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
)

data class TransferLockedAccounts(
    val fromAccount: TransferAccountSnapshot,
    val toAccount: TransferAccountSnapshot,
)

data class TransferBalanceChangeCommand(
    val memberId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val fromBalance: Money,
    val toBalance: Money,
)
