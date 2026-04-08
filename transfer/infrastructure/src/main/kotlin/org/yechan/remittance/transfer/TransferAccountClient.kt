package org.yechan.remittance.transfer

import java.math.BigDecimal

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
    val balance: BigDecimal,
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
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal,
)
