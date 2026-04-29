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
import org.yechan.remittance.account.AccountInternalTransferBalanceChangeCommand
import org.yechan.remittance.account.AccountInternalTransferBalanceChangeResult
import org.yechan.remittance.account.AccountInternalUpdateUseCase
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeResponse
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeResponse

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
                org.yechan.remittance.Money.of(request.fromBalance),
                org.yechan.remittance.Money.of(request.toBalance),
            ),
        ),
    )

    @PostMapping("/transfer-balance-change")
    fun applyTransferBalanceChange(
        @LoginUserId memberId: Long,
        @RequestBody request: AccountTransferBalanceChangeRequest,
    ): AccountTransferBalanceChangeResponse = accountInternalUpdateUseCase.applyTransferBalanceChange(
        memberId,
        AccountInternalTransferBalanceChangeCommand(
            request.fromAccountId,
            request.toAccountId,
            org.yechan.remittance.Money.of(request.debitAmount),
            org.yechan.remittance.Money.of(request.creditAmount),
        ),
    ).toResponse()

    private fun AccountInternalSnapshotValue.toResponse(): AccountSnapshotResponse = AccountSnapshotResponse(
        accountId = accountId,
        memberId = memberId,
        balance = balance.amount,
    )

    private fun AccountInternalLockValue.toResponse(): AccountLockResponse = AccountLockResponse(
        fromAccount = fromAccount.toResponse(),
        toAccount = toAccount.toResponse(),
    )

    private fun AccountInternalTransferBalanceChangeResult.toResponse(): AccountTransferBalanceChangeResponse = AccountTransferBalanceChangeResponse(
        status = status.name,
        fromAccount = fromAccount?.toResponse(),
        toAccount = toAccount?.toResponse(),
    )
}
