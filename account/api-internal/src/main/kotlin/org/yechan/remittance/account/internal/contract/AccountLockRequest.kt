package org.yechan.remittance.account.internal.contract

data class AccountLockRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
)
