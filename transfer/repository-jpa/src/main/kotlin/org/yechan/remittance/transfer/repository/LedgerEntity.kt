package org.yechan.remittance.transfer.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.yechan.remittance.BaseEntity
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
open class LedgerEntity protected constructor() :
    BaseEntity(),
    LedgerModel {
    @field:Column(name = "transfer_id", nullable = false)
    override var transferId: Long = 0
        protected set

    @field:Column(name = "account_id", nullable = false)
    override var accountId: Long = 0
        protected set

    @field:Column(nullable = false)
    override var amount: BigDecimal = BigDecimal.ZERO
        protected set

    @field:Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    override var side: LedgerProps.LedgerSideValue = LedgerProps.LedgerSideValue.DEBIT
        protected set

    private constructor(
        transferId: Long,
        accountId: Long,
        amount: BigDecimal,
        side: LedgerProps.LedgerSideValue,
    ) : this() {
        this.transferId = transferId
        this.accountId = accountId
        this.amount = amount
        this.side = side
    }

    override val ledgerId: Long?
        get() = id

    companion object {
        fun create(props: LedgerProps): LedgerEntity = LedgerEntity(props.transferId, props.accountId, props.amount, props.side)
    }
}
