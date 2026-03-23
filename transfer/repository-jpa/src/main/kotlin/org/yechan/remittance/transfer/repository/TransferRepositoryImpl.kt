package org.yechan.remittance.transfer.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.yechan.remittance.account.AccountIdentifier
import org.yechan.remittance.transfer.TransferIdentifier
import org.yechan.remittance.transfer.TransferModel
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferQueryCondition
import org.yechan.remittance.transfer.TransferRepository
import org.yechan.remittance.transfer.TransferRequestProps
import java.math.BigDecimal
import java.time.LocalDateTime

class TransferRepositoryImpl(
    private val repository: TransferJpaRepository,
) : TransferRepository {
    override fun save(props: TransferRequestProps): TransferModel = repository.save(TransferEntity.create(TransferCreateCommand(props)))

    override fun findById(identifier: TransferIdentifier): TransferModel? = repository.findById(requireNotNull(identifier.transferId)).orElse(null)

    override fun findCompletedByAccountId(
        identifier: AccountIdentifier,
        condition: TransferQueryCondition,
    ): List<TransferModel> {
        val limit = condition.limit
        val pageable = if (limit == null) Pageable.unpaged() else PageRequest.of(0, limit)
        return repository.findCompletedByAccountId(
            requireNotNull(identifier.accountId),
            COMPLETED_STATUSES,
            condition.from,
            condition.to,
            pageable,
        ).map { it as TransferModel }
    }

    override fun sumAmountByFromAccountIdAndScopeBetween(
        identifier: AccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        from: LocalDateTime,
        to: LocalDateTime,
    ): BigDecimal = repository.sumAmountByFromAccountIdAndScopeBetween(
        requireNotNull(identifier.accountId),
        scope,
        TransferProps.TransferStatusValue.SUCCEEDED,
        from,
        to,
    )

    private data class TransferCreateCommand(
        private val props: TransferRequestProps,
    ) : TransferProps {
        override val fromAccountId: Long
            get() = props.fromAccountId
        override val toAccountId: Long
            get() = props.toAccountId
        override val amount: BigDecimal
            get() = props.amount
        override val scope: TransferProps.TransferScopeValue
            get() = props.scope
        override val status: TransferProps.TransferStatusValue
            get() = TransferProps.TransferStatusValue.SUCCEEDED
        override val requestedAt: LocalDateTime
            get() = LocalDateTime.now()
        override val completedAt: LocalDateTime?
            get() = LocalDateTime.now()
    }

    private companion object {
        val COMPLETED_STATUSES = listOf(
            TransferProps.TransferStatusValue.SUCCEEDED,
            TransferProps.TransferStatusValue.FAILED,
        )
    }
}
