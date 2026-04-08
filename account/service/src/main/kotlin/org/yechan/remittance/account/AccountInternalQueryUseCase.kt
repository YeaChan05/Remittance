package org.yechan.remittance.account

import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

interface AccountInternalQueryUseCase {
    fun get(
        memberId: Long,
        accountId: Long,
    ): AccountInternalSnapshotValue?

    fun lock(
        memberId: Long,
        fromAccountId: Long,
        toAccountId: Long,
    ): AccountInternalLockValue?
}

data class AccountInternalSnapshotValue(
    val accountId: Long,
    val memberId: Long,
    val balance: BigDecimal,
)

data class AccountInternalLockValue(
    val fromAccount: AccountInternalSnapshotValue,
    val toAccount: AccountInternalSnapshotValue,
)

open class AccountInternalQueryService(
    private val accountRepository: AccountRepository,
) : AccountInternalQueryUseCase {
    @Transactional(readOnly = true)
    override fun get(
        memberId: Long,
        accountId: Long,
    ): AccountInternalSnapshotValue? = accountRepository.findById(AccountId(accountId))?.toSnapshot()

    @Transactional
    override fun lock(
        memberId: Long,
        fromAccountId: Long,
        toAccountId: Long,
    ): AccountInternalLockValue? {
        if (fromAccountId == toAccountId) {
            val account =
                accountRepository.findByIdForUpdate(AccountId(fromAccountId)) ?: return null
            val snapshot = account.toSnapshot()
            return AccountInternalLockValue(snapshot, snapshot)
        }

        val firstAccount =
            accountRepository.findByIdForUpdate(AccountId(minOf(fromAccountId, toAccountId)))
                ?: return null
        val secondAccount =
            accountRepository.findByIdForUpdate(AccountId(maxOf(fromAccountId, toAccountId)))
                ?: return null
        val fromAccount =
            if (fromAccountId == firstAccount.accountId) firstAccount else secondAccount
        val toAccount = if (toAccountId == firstAccount.accountId) firstAccount else secondAccount

        return AccountInternalLockValue(fromAccount.toSnapshot(), toAccount.toSnapshot())
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
