package org.yechan.remittance.account

import io.github.oshai.kotlinlogging.KotlinLogging

fun interface AccountCreateUseCase {
    fun create(props: AccountProps): AccountModel
}

class AccountService(
    private val accountRepository: AccountRepository
) : AccountCreateUseCase {
    private val log = KotlinLogging.logger {}

    override fun create(props: AccountProps): AccountModel {
        log.info { "account.create.start memberId=${props.memberId} bankCode=${props.bankCode}" }
        accountRepository.findByMemberIdAndBankCodeAndAccountNumber(
            props.memberId,
            props.bankCode,
            props.accountNumber
        )?.let {
            log.warn { "account.create.duplicate memberId=${props.memberId} bankCode=${props.bankCode}" }
            throw AccountDuplicateException("Account already exists")
        }
        log.info { "account.create.persist memberId=${props.memberId} bankCode=${props.bankCode}" }
        return accountRepository.save(props)
    }
}
