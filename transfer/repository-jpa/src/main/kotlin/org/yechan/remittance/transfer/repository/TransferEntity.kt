package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.DynamicUpdate
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.Money
import org.yechan.remittance.transfer.TransferModel
import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@DynamicUpdate
@Table(name = "transfer", catalog = "core")
class TransferEntity() :
    BaseEntity(),
    TransferModel {
    override val transferId: Long?
        get() = id

    @field:Column(nullable = false)
    override var fromAccountId: Long = 0

    @field:Column(nullable = false)
    override var toAccountId: Long = 0

    @field:Column(name = "amount", nullable = false)
    private var persistedAmount: BigDecimal = BigDecimal.ZERO

    @get:Transient
    override var amount: Money
        get() = Money.of(persistedAmount)
        set(value) {
            persistedAmount = value.amount
        }

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    override var scope: TransferProps.TransferScopeValue = TransferProps.TransferScopeValue.TRANSFER

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    override var status: TransferProps.TransferStatusValue =
        TransferProps.TransferStatusValue.SUCCEEDED

    @field:Column(nullable = false)
    override var requestedAt: LocalDateTime = LocalDateTime.now()

    @field:Column
    override var completedAt: LocalDateTime? = null

    private constructor(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Money,
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
