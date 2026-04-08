package org.yechan.remittance.account

import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

fun interface AccountInternalUpdateUseCase {
    fun applyBalanceChange(
        memberId: Long,
        command: AccountInternalBalanceChangeCommand,
    ): Boolean
}

data class AccountInternalBalanceChangeCommand(
    val fromAccountId: Long,
    val toAccountId: Long,
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal,
) {
    fun isSameAccount(): Boolean = this.fromAccountId == this.toAccountId
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

    private data class AccountId(
        override val accountId: Long?,
    ) : AccountIdentifier
}
