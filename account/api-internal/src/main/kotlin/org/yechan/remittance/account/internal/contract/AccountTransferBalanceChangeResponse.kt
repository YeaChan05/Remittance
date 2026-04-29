package org.yechan.remittance.account.internal.contract

data class AccountTransferBalanceChangeResponse(
    val status: String,
    val fromAccount: AccountSnapshotResponse? = null,
    val toAccount: AccountSnapshotResponse? = null,
)
