package org.yechan.remittance.account

import org.springframework.transaction.annotation.Transactional
import org.yechan.remittance.Money

interface AccountInternalUpdateUseCase {
    fun applyBalanceChange(
        memberId: Long,
        command: AccountInternalBalanceChangeCommand,
    ): Boolean

    fun applyTransferBalanceChange(
        memberId: Long,
        command: AccountInternalTransferBalanceChangeCommand,
    ): AccountInternalTransferBalanceChangeResult
}

data class AccountInternalBalanceChangeCommand(
    val fromAccountId: Long,
    val toAccountId: Long,
    val fromBalance: Money,
    val toBalance: Money,
) {
    fun isSameAccount(): Boolean = this.fromAccountId == this.toAccountId
}

data class AccountInternalTransferBalanceChangeCommand(
    val fromAccountId: Long,
    val toAccountId: Long,
    val debitAmount: Money,
    val creditAmount: Money,
) {
    fun isSameAccount(): Boolean = this.fromAccountId == this.toAccountId
}

data class AccountInternalTransferBalanceChangeResult(
    val status: AccountInternalTransferBalanceChangeStatusValue,
    val fromAccount: AccountInternalSnapshotValue? = null,
    val toAccount: AccountInternalSnapshotValue? = null,
) {
    companion object {
        fun applied(
            fromAccount: AccountInternalSnapshotValue,
            toAccount: AccountInternalSnapshotValue,
        ): AccountInternalTransferBalanceChangeResult = AccountInternalTransferBalanceChangeResult(
            AccountInternalTransferBalanceChangeStatusValue.APPLIED,
            fromAccount,
            toAccount,
        )

        fun failed(status: AccountInternalTransferBalanceChangeStatusValue): AccountInternalTransferBalanceChangeResult = AccountInternalTransferBalanceChangeResult(status)
    }
}

enum class AccountInternalTransferBalanceChangeStatusValue {
    APPLIED,
    ACCOUNT_NOT_FOUND,
    OWNER_MISMATCH,
    INSUFFICIENT_BALANCE,
}

open class AccountInternalUpdateService(
    private val accountRepository: AccountRepository,
) : AccountInternalUpdateUseCase {
    @Transactional
    override fun applyBalanceChange(
        memberId: Long,
        command: AccountInternalBalanceChangeCommand,
    ): Boolean {
        if (command.isSameAccount()) {
            val account = accountRepository.findByIdForUpdate(AccountId(command.fromAccountId))
                ?: return false
            account.updateBalance(command.toBalance)
            return true
        }

        val fromAccount =
            accountRepository.findByIdForUpdate(AccountId(command.fromAccountId)) ?: return false
        val toAccount =
            accountRepository.findByIdForUpdate(AccountId(command.toAccountId)) ?: return false
        fromAccount.updateBalance(command.fromBalance)
        toAccount.updateBalance(command.toBalance)
        return true
    }

    @Transactional
    override fun applyTransferBalanceChange(
        memberId: Long,
        command: AccountInternalTransferBalanceChangeCommand,
    ): AccountInternalTransferBalanceChangeResult {
        if (command.isSameAccount()) {
            val account = accountRepository.findByIdForUpdate(AccountId(command.fromAccountId))
                ?: return AccountInternalTransferBalanceChangeResult.failed(
                    AccountInternalTransferBalanceChangeStatusValue.ACCOUNT_NOT_FOUND,
                )
            if (account.memberId != memberId) {
                return AccountInternalTransferBalanceChangeResult.failed(
                    AccountInternalTransferBalanceChangeStatusValue.OWNER_MISMATCH,
                )
            }
            if (account.balance < command.debitAmount) {
                return AccountInternalTransferBalanceChangeResult.failed(
                    AccountInternalTransferBalanceChangeStatusValue.INSUFFICIENT_BALANCE,
                )
            }
            account.updateBalance(
                account.balance.subtract(command.debitAmount).add(command.creditAmount),
            )
            val snapshot = account.toSnapshot()
            return AccountInternalTransferBalanceChangeResult.applied(snapshot, snapshot)
        }

        val firstAccount =
            accountRepository.findByIdForUpdate(
                AccountId(
                    minOf(
                        command.fromAccountId,
                        command.toAccountId,
                    ),
                ),
            )
                ?: return AccountInternalTransferBalanceChangeResult.failed(
                    AccountInternalTransferBalanceChangeStatusValue.ACCOUNT_NOT_FOUND,
                )
        val secondAccount =
            accountRepository.findByIdForUpdate(
                AccountId(
                    maxOf(
                        command.fromAccountId,
                        command.toAccountId,
                    ),
                ),
            )
                ?: return AccountInternalTransferBalanceChangeResult.failed(
                    AccountInternalTransferBalanceChangeStatusValue.ACCOUNT_NOT_FOUND,
                )
        val fromAccount =
            if (command.fromAccountId == firstAccount.accountId) firstAccount else secondAccount
        val toAccount =
            if (command.toAccountId == firstAccount.accountId) firstAccount else secondAccount

        if (fromAccount.memberId != memberId) {
            return AccountInternalTransferBalanceChangeResult.failed(
                AccountInternalTransferBalanceChangeStatusValue.OWNER_MISMATCH,
            )
        }
        if (fromAccount.balance < command.debitAmount) {
            return AccountInternalTransferBalanceChangeResult.failed(
                AccountInternalTransferBalanceChangeStatusValue.INSUFFICIENT_BALANCE,
            )
        }

        fromAccount.updateBalance(fromAccount.balance.subtract(command.debitAmount))
        toAccount.updateBalance(toAccount.balance.add(command.creditAmount))
        return AccountInternalTransferBalanceChangeResult.applied(
            fromAccount.toSnapshot(),
            toAccount.toSnapshot(),
        )
    }

    private fun AccountModel.toSnapshot(): AccountInternalSnapshotValue = AccountInternalSnapshotValue(
        accountId = requireNotNull(accountId),
        memberId = requireNotNull(memberId),
        balance = balance,
    )

    private data class AccountId(
        override val accountId: Long?,
    ) : AccountIdentifier
}
