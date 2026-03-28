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
class AccountEntity() :
    BaseEntity(),
    AccountModel {
    override val accountId: Long?
        get() = id

    @field:Column(nullable = false)
    override var memberId: Long? = null

    @field:Column(nullable = false)
    override var bankCode: String = ""

    @field:Column(nullable = false)
    override var accountNumber: String = ""

    @field:Column(nullable = false)
    override var accountName: String = ""

    @field:Column(nullable = false)
    override var balance: BigDecimal = BigDecimal.ZERO

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
