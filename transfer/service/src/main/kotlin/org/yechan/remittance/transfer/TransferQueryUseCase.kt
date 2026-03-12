package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.yechan.remittance.account.AccountIdentifier
import org.yechan.remittance.account.AccountRepository

fun interface TransferQueryUseCase {
    fun query(
        memberId: Long,
        accountId: Long,
        condition: TransferQueryCondition
    ): List<TransferModel>
}

private val log = KotlinLogging.logger {}

class TransferQueryService(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository
) : TransferQueryUseCase {
    override fun query(
        memberId: Long,
        accountId: Long,
        condition: TransferQueryCondition
    ): List<TransferModel> {
        log.info { "transfer.query.start memberId=$memberId accountId=$accountId" }
        val account =
            accountRepository.findById(AccountId(accountId))
                ?: run {
                    log.warn { "transfer.query.account_not_found accountId=$accountId" }
                    throw TransferFailedException(TransferFailureCode.ACCOUNT_NOT_FOUND, "Account not found")
                }

        if (memberId != account.memberId) {
            log.warn { "transfer.query.owner_mismatch memberId=$memberId accountId=$accountId" }
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Account owner mismatch")
        }

        log.info { "transfer.query.fetch accountId=$accountId" }
        return transferRepository.findCompletedByAccountId(AccountId(accountId), condition)
    }

    private data class AccountId(
        override val accountId: Long?
    ) : AccountIdentifier
}
