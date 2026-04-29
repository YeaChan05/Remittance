package org.yechan.remittance.transfer

import org.yechan.remittance.Money

interface TransferAccountClient {
    fun get(
        memberId: Long,
        accountId: Long,
    ): TransferAccountSnapshot?

    fun lock(command: TransferAccountLockCommand): TransferLockedAccounts?

    fun applyBalanceChange(command: TransferBalanceChangeCommand)

    fun applyTransferBalanceChange(command: TransferBalanceDeltaCommand): TransferBalanceChangeResult
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

data class TransferBalanceDeltaCommand(
    val memberId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val debitAmount: Money,
    val creditAmount: Money,
)

data class TransferBalanceChangeResult(
    val status: TransferBalanceChangeStatusValue,
    val fromAccount: TransferAccountSnapshot? = null,
    val toAccount: TransferAccountSnapshot? = null,
)

enum class TransferBalanceChangeStatusValue {
    APPLIED,
    ACCOUNT_NOT_FOUND,
    OWNER_MISMATCH,
    INSUFFICIENT_BALANCE,
}
