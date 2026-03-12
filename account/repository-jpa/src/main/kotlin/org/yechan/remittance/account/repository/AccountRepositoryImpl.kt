package org.yechan.remittance.account.repository

import org.yechan.remittance.account.AccountIdentifier
import org.yechan.remittance.account.AccountModel
import org.yechan.remittance.account.AccountProps
import org.yechan.remittance.account.AccountRepository

class AccountRepositoryImpl(
    private val repository: AccountJpaRepository
) : AccountRepository {
    override fun save(props: AccountProps): AccountModel {
        return repository.save(AccountEntity.create(props))
    }

    override fun findById(identifier: AccountIdentifier): AccountModel? {
        return repository.findById(requireNotNull(identifier.accountId)).orElse(null)
    }

    override fun findByIdForUpdate(identifier: AccountIdentifier): AccountModel? {
        return repository.findByIdForUpdate(requireNotNull(identifier.accountId))
    }

    override fun findByMemberIdAndBankCodeAndAccountNumber(
        memberId: Long?,
        bankCode: String,
        accountNumber: String
    ): AccountModel? {
        return repository.findByMemberIdAndBankCodeAndAccountNumber(
            requireNotNull(memberId),
            bankCode,
            accountNumber
        )
    }

    override fun delete(identifier: AccountIdentifier) {
        repository.deleteById(requireNotNull(identifier.accountId))
    }
}
