package org.yechan.remittance.account

import io.github.oshai.kotlinlogging.KotlinLogging

fun interface AccountDeleteUseCase {
    fun delete(props: AccountDeleteProps): AccountModel
}

private val log = KotlinLogging.logger {}

class AccountDeleteService(
    private val accountRepository: AccountRepository,
) : AccountDeleteUseCase {
    override fun delete(props: AccountDeleteProps): AccountModel {
        log.info { "account.delete.start memberId=${props.memberId} accountId=${props.accountId}" }
        val identifier = AccountId(props.accountId)
        val account =
            accountRepository.findById(identifier)
                ?: run {
                    log.warn {
                        "account.delete.not_found memberId=${props.memberId} accountId=${props.accountId}"
                    }
                    throw AccountNotFoundException("Account not found")
                }
        if (account.memberId != props.memberId) {
            log.warn {
                "account.delete.permission_denied memberId=${props.memberId} accountId=${props.accountId}"
            }
            throw AccountPermissionDeniedException("Account owner mismatch")
        }
        accountRepository.delete(identifier)
        log.info {
            "account.delete.success memberId=${props.memberId} accountId=${props.accountId}"
        }
        return account
    }

    private data class AccountId(
        override val accountId: Long?,
    ) : AccountIdentifier
}
