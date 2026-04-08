package org.yechan.remittance.account.internal.contract

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import org.yechan.remittance.InternalRequestHeaders

@HttpExchange("/internal/accounts")
interface AccountInternalApi {
    @PostExchange("/query")
    fun get(
        @RequestHeader(InternalRequestHeaders.USER_ID) memberId: Long,
        @RequestBody request: AccountGetRequest,
    ): AccountSnapshotResponse?

    @PostExchange("/lock")
    fun lock(
        @RequestHeader(InternalRequestHeaders.USER_ID) memberId: Long,
        @RequestBody request: AccountLockRequest,
    ): AccountLockResponse?

    @PostExchange("/balance-change")
    fun applyBalanceChange(
        @RequestHeader(InternalRequestHeaders.USER_ID) memberId: Long,
        @RequestBody request: AccountBalanceChangeRequest,
    ): AccountBalanceChangeResponse
}
