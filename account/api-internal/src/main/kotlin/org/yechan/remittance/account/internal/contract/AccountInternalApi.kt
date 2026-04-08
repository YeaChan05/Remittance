package org.yechan.remittance.account.internal.contract

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

private const val INTERNAL_USER_ID_HEADER = "X-Internal-User-Id"

@HttpExchange("/internal/accounts")
interface AccountInternalApi {
    @PostExchange("/query")
    fun get(
        @RequestHeader(INTERNAL_USER_ID_HEADER) memberId: Long,
        @RequestBody request: AccountGetRequest,
    ): AccountSnapshotResponse?

    @PostExchange("/lock")
    fun lock(
        @RequestHeader(INTERNAL_USER_ID_HEADER) memberId: Long,
        @RequestBody request: AccountLockRequest,
    ): AccountLockResponse?

    @PostExchange("/balance-change")
    fun applyBalanceChange(
        @RequestHeader(INTERNAL_USER_ID_HEADER) memberId: Long,
        @RequestBody request: AccountBalanceChangeRequest,
    ): AccountBalanceChangeResponse
}
