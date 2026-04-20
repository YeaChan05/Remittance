package org.yechan.remittance.account

import org.yechan.remittance.Money

interface AccountProps {
    val memberId: Long?
    val bankCode: String
    val accountNumber: String
    val accountName: String
    val balance: Money
}
