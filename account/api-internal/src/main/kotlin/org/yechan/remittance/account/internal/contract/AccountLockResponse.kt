package org.yechan.remittance.account.internal.contract

data class AccountLockResponse(
    val fromAccount: AccountSnapshotResponse,
    val toAccount: AccountSnapshotResponse,
)
