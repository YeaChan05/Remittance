package org.yechan.remittance.account

import org.yechan.remittance.Money

data class Account(
    override val accountId: Long?,
    override val memberId: Long?,
    override val bankCode: String,
    override val accountNumber: String,
    override val accountName: String,
    override var balance: Money,
) : AccountModel
