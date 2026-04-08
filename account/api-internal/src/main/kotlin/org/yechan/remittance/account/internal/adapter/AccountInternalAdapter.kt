package org.yechan.remittance.account.internal.adapter

import org.yechan.remittance.account.AccountInternalBalanceChangeCommand
import org.yechan.remittance.account.AccountInternalLockValue
import org.yechan.remittance.account.AccountInternalQueryUseCase
import org.yechan.remittance.account.AccountInternalSnapshotValue
import org.yechan.remittance.account.AccountInternalUpdateUseCase
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountBalanceChangeResponse
import org.yechan.remittance.account.internal.contract.AccountGetRequest
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse

class AccountInternalAdapter(
    private val accountInternalQueryUseCase: AccountInternalQueryUseCase,
    private val accountInternalUpdateUseCase: AccountInternalUpdateUseCase,
) : AccountInternalApi {
    override fun get(
        memberId: Long,
        request: AccountGetRequest,
    ): AccountSnapshotResponse? = accountInternalQueryUseCase.get(memberId, request.accountId)?.toResponse()

    override fun lock(
        memberId: Long,
        request: AccountLockRequest,
    ): AccountLockResponse? = accountInternalQueryUseCase.lock(
        memberId,
        request.fromAccountId,
        request.toAccountId,
    )?.toResponse()

    override fun applyBalanceChange(
        memberId: Long,
        request: AccountBalanceChangeRequest,
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
