package org.yechan.remittance.account

interface AccountRepository {
    fun save(props: AccountProps): AccountModel

    fun findById(identifier: AccountIdentifier): AccountModel?

    fun findByIdForUpdate(identifier: AccountIdentifier): AccountModel?

    fun findByMemberIdAndBankCodeAndAccountNumber(
        memberId: Long?,
        bankCode: String,
        accountNumber: String,
    ): AccountModel?

    fun delete(identifier: AccountIdentifier)
}
