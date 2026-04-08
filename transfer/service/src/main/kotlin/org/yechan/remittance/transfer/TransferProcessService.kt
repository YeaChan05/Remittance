package org.yechan.remittance.transfer

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

private val log = KotlinLogging.logger {}

open class TransferProcessService(
    private val transferAccountClient: TransferAccountClient,
    private val transferRepository: TransferRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    private val dailyLimitUsageRepository: DailyLimitUsageRepository,
    private val transferMemberClient: TransferMemberClient,
    private val transferSnapshotUtil: TransferSnapshotUtil,
) {
    @Transactional
    open fun process(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps,
        now: LocalDateTime,
    ): TransferResult {
        log.info { "transfer.process.start memberId=$memberId scope=${props.scope}" }
        validateTransferRequest(props)
        val accounts = lockAccounts(memberId, props)
        validateOwner(memberId, accounts)
        validateDailyLimit(props, now)
        val balanceChange = calculateBalanceChange(memberId, props, accounts)
        transferAccountClient.applyBalanceChange(balanceChange)
        return persistTransfer(memberId, idempotencyKey, props, now)
    }

    private fun validateTransferRequest(props: TransferRequestProps) {
        if (props.scope == TransferProps.TransferScopeValue.TRANSFER &&
            props.fromAccountId == props.toAccountId
        ) {
            log.warn { "transfer.process.same_account fromAccountId=${props.fromAccountId}" }
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Same account")
        }
    }

    private fun lockAccounts(
        memberId: Long,
        props: TransferRequestProps,
    ): AccountPair = transferAccountClient.lock(
        TransferAccountLockCommand(
            memberId = memberId,
            fromAccountId = props.fromAccountId,
            toAccountId = props.toAccountId,
        ),
    )?.let {
        AccountPair(it.fromAccount, it.toAccount)
    } ?: run {
        log.warn { "transfer.process.account_not_found fromAccountId=${props.fromAccountId} toAccountId=${props.toAccountId}" }
        throw TransferFailedException(TransferFailureCode.ACCOUNT_NOT_FOUND, "Account not found")
    }

    private fun validateOwner(memberId: Long, accounts: AccountPair) {
        val fromMemberId = accounts.fromAccount.memberId
        val toMemberId = accounts.toAccount.memberId
        if (!transferMemberClient.exists(fromMemberId)) {
            log.warn { "transfer.process.owner_not_found fromMemberId=$fromMemberId" }
            throw TransferFailedException(TransferFailureCode.OWNER_NOT_FOUND, "Owner not found")
        }
        if (!transferMemberClient.exists(toMemberId)) {
            log.warn { "transfer.process.receiver_member_not_found toMemberId=$toMemberId" }
            throw TransferFailedException(
                TransferFailureCode.MEMBER_NOT_FOUND,
                "Sending account's member not found",
            )
        }
        if (memberId != fromMemberId) {
            log.warn { "transfer.process.owner_mismatch memberId=$memberId fromMemberId=$fromMemberId" }
            throw TransferFailedException(
                TransferFailureCode.INVALID_REQUEST,
                "Account owner mismatch",
            )
        }
    }

    private fun calculateBalanceChange(
        memberId: Long,
        props: TransferRequestProps,
        accounts: AccountPair,
    ): TransferBalanceChangeCommand {
        if (props.scope == TransferProps.TransferScopeValue.DEPOSIT) {
            val balance = accounts.toAccount.balance.add(props.amount)
            return TransferBalanceChangeCommand(
                memberId = memberId,
                fromAccountId = accounts.fromAccount.accountId,
                toAccountId = accounts.toAccount.accountId,
                fromBalance = balance,
                toBalance = balance,
            )
        }
        if (accounts.isInsufficient(props.debit())) {
            log.warn { "transfer.process.insufficient_balance fromAccountId=${accounts.fromAccount.accountId}" }
            throw TransferFailedException(
                TransferFailureCode.INSUFFICIENT_BALANCE,
                "Insufficient balance",
            )
        }
        val debit = props.debit()
        if (props.scope == TransferProps.TransferScopeValue.WITHDRAW) {
            val balance = accounts.fromAccount.balance.subtract(debit)
            return TransferBalanceChangeCommand(
                memberId = memberId,
                fromAccountId = accounts.fromAccount.accountId,
                toAccountId = accounts.toAccount.accountId,
                fromBalance = balance,
                toBalance = balance,
            )
        }
        return TransferBalanceChangeCommand(
            memberId = memberId,
            fromAccountId = accounts.fromAccount.accountId,
            toAccountId = accounts.toAccount.accountId,
            fromBalance = accounts.fromAccount.balance.subtract(debit),
            toBalance = accounts.toAccount.balance.add(props.amount),
        )
    }

    private fun validateDailyLimit(props: TransferRequestProps, now: LocalDateTime) {
        if (props.scope == TransferProps.TransferScopeValue.DEPOSIT) {
            return
        }
        val usage = dailyLimitUsageRepository.findOrCreateForUpdate(
            AccountId(props.fromAccountId),
            props.scope,
            now.toLocalDate(),
        )
        val limit =
            if (props.scope == TransferProps.TransferScopeValue.WITHDRAW) WITHDRAW_DAILY_LIMIT else TRANSFER_DAILY_LIMIT
        val nextUsed = usage.usedAmount.add(props.amount)
        if (nextUsed > limit) {
            log.warn { "transfer.process.daily_limit_exceeded fromAccountId=${props.fromAccountId} scope=${props.scope}" }
            throw TransferFailedException(
                TransferFailureCode.DAILY_LIMIT_EXCEEDED,
                "Daily limit exceeded",
            )
        }
        usage.updateUsedAmount(nextUsed)
    }

    private fun persistTransfer(
        memberId: Long,
        idempotencyKey: String,
        props: TransferRequestProps,
        now: LocalDateTime,
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
            now,
        )
        return result
    }

    private data class OutboxEventCreateCommand(
        private val transfer: TransferModel,
        override val payload: String,
    ) : OutboxEventProps {
        override val aggregateType: String = AGGREGATE_TYPE
        override val aggregateId: String
            get() = requireNotNull(transfer.transferId).toString()
        override val eventType: String = EVENT_TYPE
        override val status: OutboxEventProps.OutboxEventStatusValue =
            OutboxEventProps.OutboxEventStatusValue.NEW
    }

    private data class AccountPair(
        val fromAccount: TransferAccountSnapshot,
        val toAccount: TransferAccountSnapshot,
    ) {
        fun isInsufficient(debit: BigDecimal): Boolean = fromAccount.balance < debit
    }

    private data class AccountId(override val accountId: Long?) : TransferAccountIdentifier

    private companion object {
        val WITHDRAW_DAILY_LIMIT: BigDecimal = BigDecimal.valueOf(1_000_000)
        val TRANSFER_DAILY_LIMIT: BigDecimal = BigDecimal.valueOf(3_000_000)
        const val AGGREGATE_TYPE = "TRANSFER"
        const val EVENT_TYPE = "TRANSFER_COMPLETED"
    }
}
