package org.yechan.remittance.account

import java.math.BigDecimal

interface AccountProps {
    val memberId: Long?
    val bankCode: String
    val accountNumber: String
    val accountName: String
    val balance: BigDecimal
}
