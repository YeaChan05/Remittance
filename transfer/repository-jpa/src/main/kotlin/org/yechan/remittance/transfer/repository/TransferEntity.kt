package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.transfer.TransferModel
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "transfer", catalog = "core")
class TransferEntity protected constructor() :
    BaseEntity(),
    TransferModel {
    @field:Column(nullable = false)
    override var fromAccountId: Long = 0
        protected set

    @field:Column(nullable = false)
    override var toAccountId: Long = 0
        protected set

    @field:Column(nullable = false)
    override var amount: BigDecimal = BigDecimal.ZERO
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    override var scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    override var status: TransferProps.TransferStatusValue = TransferProps.TransferStatusValue.SUCCEEDED
        protected set

    @field:Column(nullable = false)
    override var requestedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @field:Column
    override var completedAt: LocalDateTime? = null
        protected set

    private constructor(
        fromAccountId: Long,
        toAccountId: Long,
        amount: BigDecimal,
        scope: TransferProps.TransferScopeValue,
        status: TransferProps.TransferStatusValue,
        completedAt: LocalDateTime?,
    ) : this() {
        this.fromAccountId = fromAccountId
        this.toAccountId = toAccountId
        this.amount = amount
        this.scope = scope
        this.status = status
        this.requestedAt = LocalDateTime.now()
        this.completedAt = completedAt
    }

    override val transferId: Long?
        get() = id

    companion object {
        fun create(props: TransferProps): TransferEntity = TransferEntity(
            props.fromAccountId,
            props.toAccountId,
            props.amount,
            props.scope,
            props.status,
            props.completedAt,
        )
    }
}
