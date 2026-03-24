package org.yechan.remittance.account

import java.math.BigDecimal

fun interface AccountInternalUpdateUseCase {
    fun applyBalanceChange(command: AccountInternalBalanceChangeCommand): Boolean
}

data class AccountInternalBalanceChangeCommand(
    val fromAccountId: Long,
    val toAccountId: Long,
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal,
)

class AccountInternalUpdateService(
    private val accountRepository: AccountRepository,
) : AccountInternalUpdateUseCase {
    override fun applyBalanceChange(command: AccountInternalBalanceChangeCommand): Boolean {
        if (command.fromAccountId == command.toAccountId) {
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

    private data class AccountId(
        override val accountId: Long?,
    ) : AccountIdentifier
}
