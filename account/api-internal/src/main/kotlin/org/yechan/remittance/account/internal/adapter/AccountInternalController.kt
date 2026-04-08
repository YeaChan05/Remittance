package org.yechan.remittance.account.internal.adapter

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.LoginUserId
import org.yechan.remittance.account.AccountInternalBalanceChangeCommand
import org.yechan.remittance.account.AccountInternalLockValue
import org.yechan.remittance.account.AccountInternalQueryUseCase
import org.yechan.remittance.account.AccountInternalSnapshotValue
import org.yechan.remittance.account.AccountInternalUpdateUseCase
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeResponse
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse

@RestController
@RequestMapping("/internal/accounts")
class AccountInternalController(
    private val accountInternalQueryUseCase: AccountInternalQueryUseCase,
    private val accountInternalUpdateUseCase: AccountInternalUpdateUseCase,
) {
    @PostMapping("/query")
    fun get(
        @LoginUserId memberId: Long,
        @RequestBody request: AccountGetRequest,
    ): AccountSnapshotResponse? = accountInternalQueryUseCase.get(memberId, request.accountId)?.toResponse()

    @PostMapping("/lock")
    fun lock(
        @LoginUserId memberId: Long,
        @RequestBody request: AccountLockRequest,
    ): AccountLockResponse? = accountInternalQueryUseCase.lock(
        memberId,
        request.fromAccountId,
        request.toAccountId,
    )?.toResponse()

    @PostMapping("/balance-change")
    fun applyBalanceChange(
        @LoginUserId memberId: Long,
        @RequestBody request: AccountBalanceChangeRequest,
    ): AccountBalanceChangeResponse = AccountBalanceChangeResponse(
        accountInternalUpdateUseCase.applyBalanceChange(
            memberId,
            AccountInternalBalanceChangeCommand(
                request.fromAccountId,
                request.toAccountId,
                request.fromBalance,
                request.toBalance,
            ),
        ),
    )

    private fun AccountInternalSnapshotValue.toResponse(): AccountSnapshotResponse = AccountSnapshotResponse(
        accountId = accountId,
        memberId = memberId,
        balance = balance,
    )

    private fun AccountInternalLockValue.toResponse(): AccountLockResponse = AccountLockResponse(
        fromAccount = fromAccount.toResponse(),
        toAccount = toAccount.toResponse(),
    )
}
