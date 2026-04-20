package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.Money
import org.yechan.remittance.transfer.LedgerModel
import org.yechan.remittance.transfer.LedgerProps
import java.math.BigDecimal

@Entity
@Table(
    name = "ledger",
    catalog = "core",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_ledger_transfer_account_side",
            columnNames = ["transfer_id", "account_id", "side"],
        ),
    ],
)
class LedgerEntity() :
    BaseEntity(),
    LedgerModel {
    override val ledgerId: Long?
        get() = id

    @field:Column(name = "transfer_id", nullable = false)
    override var transferId: Long = 0

    @field:Column(name = "account_id", nullable = false)
    override var accountId: Long = 0

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
    override var side: LedgerProps.LedgerSideValue = LedgerProps.LedgerSideValue.DEBIT

    private constructor(
        transferId: Long,
        accountId: Long,
        amount: Money,
        side: LedgerProps.LedgerSideValue,
    ) : this() {
        this.transferId = transferId
        this.accountId = accountId
        this.amount = amount
        this.side = side
    }

    companion object {
        fun create(props: LedgerProps): LedgerEntity = LedgerEntity(props.transferId, props.accountId, props.amount, props.side)
    }
}
