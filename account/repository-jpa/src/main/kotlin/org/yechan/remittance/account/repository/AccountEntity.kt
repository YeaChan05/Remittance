package org.yechan.remittance.account.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.yechan.remittance.BaseEntity
import org.yechan.remittance.account.AccountModel
import org.yechan.remittance.account.AccountProps
import java.math.BigDecimal

@Entity
@Table(name = "account", catalog = "core")
open class AccountEntity protected constructor() :
    BaseEntity(),
    AccountModel {
    @field:Column(nullable = false)
    override var memberId: Long? = null
        protected set

    @field:Column(nullable = false)
    override var bankCode: String = ""
        protected set

    @field:Column(nullable = false)
    override var accountNumber: String = ""
        protected set

    @field:Column(nullable = false)
    override var accountName: String = ""
        protected set

    @field:Column(nullable = false)
    override var balance: BigDecimal = BigDecimal.ZERO
        protected set

    private constructor(
        memberId: Long?,
        bankCode: String,
        accountNumber: String,
        accountName: String,
        balance: BigDecimal,
    ) : this() {
        this.memberId = memberId
        this.bankCode = bankCode
        this.accountNumber = accountNumber
        this.accountName = accountName
        this.balance = balance
    }

    override val accountId: Long?
        get() = id

    override fun updateBalance(balance: BigDecimal) {
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
