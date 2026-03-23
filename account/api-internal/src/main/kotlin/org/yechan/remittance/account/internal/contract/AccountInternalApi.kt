package org.yechan.remittance.account.internal.contract

interface AccountInternalApi {
    fun get(request: AccountGetRequest): AccountSnapshotResponse?

    fun lock(request: AccountLockRequest): AccountLockResponse?

    fun applyBalanceChange(request: AccountBalanceChangeRequest): AccountBalanceChangeResponse
}
