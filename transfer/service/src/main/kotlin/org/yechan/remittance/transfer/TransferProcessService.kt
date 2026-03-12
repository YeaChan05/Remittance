package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.transaction.annotation.Transactional
import org.yechan.remittance.account.AccountIdentifier
import org.yechan.remittance.account.AccountModel
import org.yechan.remittance.account.AccountRepository
import org.yechan.remittance.member.MemberIdentifier
import org.yechan.remittance.member.MemberRepository

private val log = KotlinLogging.logger {}

open class TransferProcessService(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val dailyLimitUsageRepository: DailyLimitUsageRepository,
    private val memberRepository: MemberRepository,
    private val transferSnapshotUtil: TransferSnapshotUtil
) {
    @Transactional
    open fun process(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps,
        now: LocalDateTime
    ): TransferResult {
        log.info { "transfer.process.start memberId=$memberId scope=${props.scope}" }
        val accounts = lockAccounts(props)
        validateOwner(memberId, accounts)
        validateDailyLimit(props, now)
        validateBalance(props, accounts)
        updateBalances(props, accounts)
        return persistTransfer(memberId, idempotencyKey, props, now)
    }

    private fun lockAccounts(props: TransferRequestProps): AccountPair {
        val fromAccountId = props.fromAccountId
        val toAccountId = props.toAccountId
        if (props.scope == TransferProps.TransferScopeValue.WITHDRAW ||
            props.scope == TransferProps.TransferScopeValue.DEPOSIT
        ) {
            val fromAccount = getAccountForUpdate(fromAccountId)
            return AccountPair(fromAccount, fromAccount)
        }
        if (fromAccountId == toAccountId) {
            log.warn { "transfer.process.same_account fromAccountId=$fromAccountId" }
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Same account")
        }

        val firstAccount = getAccountForUpdate(minOf(fromAccountId, toAccountId))
        val secondAccount = getAccountForUpdate(maxOf(fromAccountId, toAccountId))
        val fromAccount = if (fromAccountId == firstAccount.accountId) firstAccount else secondAccount
        val toAccount = if (toAccountId == firstAccount.accountId) firstAccount else secondAccount

        return AccountPair(fromAccount, toAccount)
    }

    private fun getAccountForUpdate(accountId: Long): AccountModel {
        return accountRepository.findByIdForUpdate(AccountId(accountId))
            ?: run {
                log.warn { "transfer.process.account_not_found accountId=$accountId" }
                throw TransferFailedException(TransferFailureCode.ACCOUNT_NOT_FOUND, "Account not found")
            }
    }

    private fun validateOwner(memberId: Long, accounts: AccountPair) {
        val fromMemberId = requireNotNull(accounts.fromAccount.memberId)
        val toMemberId = requireNotNull(accounts.toAccount.memberId)
        memberRepository.findById(MemberId(fromMemberId))
            ?: run {
                log.warn { "transfer.process.owner_not_found fromMemberId=$fromMemberId" }
                throw TransferFailedException(TransferFailureCode.OWNER_NOT_FOUND, "Owner not found")
            }
        memberRepository.findById(MemberId(toMemberId))
            ?: run {
                log.warn { "transfer.process.receiver_member_not_found toMemberId=$toMemberId" }
                throw TransferFailedException(TransferFailureCode.MEMBER_NOT_FOUND, "Sending account's member not found")
            }
        if (memberId != fromMemberId) {
            log.warn { "transfer.process.owner_mismatch memberId=$memberId fromMemberId=$fromMemberId" }
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Account owner mismatch")
        }
    }

    private fun validateBalance(props: TransferRequestProps, accounts: AccountPair) {
        if (props.scope == TransferProps.TransferScopeValue.DEPOSIT) {
            return
        }
        if (accounts.isInsufficient(props.debit())) {
            log.warn { "transfer.process.insufficient_balance fromAccountId=${accounts.fromAccount.accountId}" }
            throw TransferFailedException(TransferFailureCode.INSUFFICIENT_BALANCE, "Insufficient balance")
        }
    }

    private fun validateDailyLimit(props: TransferRequestProps, now: LocalDateTime) {
        if (props.scope == TransferProps.TransferScopeValue.DEPOSIT) {
            return
        }
        val usage = dailyLimitUsageRepository.findOrCreateForUpdate(AccountId(props.fromAccountId), props.scope, now.toLocalDate())
        val limit = if (props.scope == TransferProps.TransferScopeValue.WITHDRAW) WITHDRAW_DAILY_LIMIT else TRANSFER_DAILY_LIMIT
        val nextUsed = usage.usedAmount.add(props.amount)
        if (nextUsed.compareTo(limit) > 0) {
            log.warn { "transfer.process.daily_limit_exceeded fromAccountId=${props.fromAccountId} scope=${props.scope}" }
            throw TransferFailedException(TransferFailureCode.DAILY_LIMIT_EXCEEDED, "Daily limit exceeded")
        }
        usage.updateUsedAmount(nextUsed)
    }

    private fun updateBalances(props: TransferRequestProps, accounts: AccountPair) {
        if (props.scope == TransferProps.TransferScopeValue.DEPOSIT) {
            accounts.toAccount.updateBalance(accounts.toAccount.balance.add(props.amount))
            return
        }
        val debit = props.debit()
        accounts.fromAccount.updateBalance(accounts.fromAccount.balance.subtract(debit))
        if (props.scope == TransferProps.TransferScopeValue.WITHDRAW) {
            return
        }
        accounts.toAccount.updateBalance(accounts.toAccount.balance.add(props.amount))
    }

    private fun persistTransfer(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps,
        now: LocalDateTime
    ): TransferResult {
        val transfer = transferRepository.save(props)
        if (props.scope == TransferProps.TransferScopeValue.TRANSFER) {
            log.info { "transfer.process.outbox.create transferId=${transfer.transferId}" }
            val payload = transferSnapshotUtil.toOutboxPayload(transfer, props, now)
            outboxEventRepository.save(OutboxEventCreateCommand(transfer, payload))
        }
        val result = TransferResult.succeeded(requireNotNull(transfer.transferId))
        idempotencyKeyRepository.markSucceeded(
            memberId,
            props.scope.toIdempotencyScope(),
            idempotencyKey,
            transferSnapshotUtil.toSnapshot(result),
            now
        )
        return result
    }

    private data class OutboxEventCreateCommand(
        private val transfer: TransferModel,
        override val payload: String
    ) : OutboxEventProps {
        override val aggregateType: String = AGGREGATE_TYPE
        override val aggregateId: String
            get() = requireNotNull(transfer.transferId).toString()
        override val eventType: String = EVENT_TYPE
        override val status: OutboxEventProps.OutboxEventStatusValue =
            OutboxEventProps.OutboxEventStatusValue.NEW
    }

    private data class AccountPair(val fromAccount: AccountModel, val toAccount: AccountModel) {
        fun isInsufficient(debit: BigDecimal): Boolean = fromAccount.balance.compareTo(debit) < 0
    }

    private data class AccountId(override val accountId: Long?) : AccountIdentifier

    private data class MemberId(override val memberId: Long?) : MemberIdentifier

    private companion object {
        val WITHDRAW_DAILY_LIMIT: BigDecimal = BigDecimal.valueOf(1_000_000)
        val TRANSFER_DAILY_LIMIT: BigDecimal = BigDecimal.valueOf(3_000_000)
        const val AGGREGATE_TYPE = "TRANSFER"
        const val EVENT_TYPE = "TRANSFER_COMPLETED"
    }
}
