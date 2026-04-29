package org.yechan.remittance.account.internal.adapter

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
import org.yechan.remittance.account.internal.contract.AccountInternalApi
import org.yechan.remittance.account.internal.contract.AccountLockRequest
import org.yechan.remittance.account.internal.contract.AccountLockResponse
import org.yechan.remittance.account.internal.contract.AccountSnapshotResponse
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeRequest
import org.yechan.remittance.account.internal.contract.AccountTransferBalanceChangeResponse

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
                org.yechan.remittance.Money.of(request.fromBalance),
                org.yechan.remittance.Money.of(request.toBalance),
            ),
        ),
    )

    override fun applyTransferBalanceChange(
        memberId: Long,
        request: AccountTransferBalanceChangeRequest,
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
