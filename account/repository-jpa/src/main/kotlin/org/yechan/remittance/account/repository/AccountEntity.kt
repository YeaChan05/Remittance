package org.yechan.remittance.account.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.annotations.DynamicUpdate
import org.hibernate.annotations.NaturalId
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.Money
import org.yechan.remittance.account.AccountModel
import org.yechan.remittance.account.AccountProps
import java.math.BigDecimal

@Entity
@DynamicUpdate
@Table(name = "account", catalog = "core")
class AccountEntity() :
    BaseEntity(),
    AccountModel {
    override val accountId: Long?
        get() = id

    @NaturalId
    @field:Column(nullable = false)
    override var memberId: Long? = null

    @NaturalId
    @field:Column(nullable = false)
    override var bankCode: String = ""

    @NaturalId
    @field:Column(nullable = false)
    override var accountNumber: String = ""

    @field:Column(nullable = false)
    override var accountName: String = ""

    @field:Column(name = "balance", nullable = false)
    private var persistedBalance: BigDecimal = BigDecimal.ZERO

    @get:Transient
    override var balance: Money
        get() = Money.of(persistedBalance)
        set(value) {
            persistedBalance = value.amount
        }

    private constructor(
        memberId: Long?,
        bankCode: String,
        accountNumber: String,
        accountName: String,
        balance: Money,
    ) : this() {
        this.memberId = memberId
        this.bankCode = bankCode
        this.accountNumber = accountNumber
        this.accountName = accountName
        this.balance = balance
    }

    override fun updateBalance(balance: Money) {
        this.balance = balance
    }

    companion object {
        fun create(props: AccountProps): AccountEntity = AccountEntity(
            props.memberId,
            props.bankCode,
            props.accountNumber,
            props.accountName,
            props.balance,
        )
    }
}
