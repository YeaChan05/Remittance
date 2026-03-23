package org.yechan.remittance.account

import java.math.BigDecimal

interface AccountModel :
    AccountProps,
    AccountIdentifier {
    fun updateBalance(balance: BigDecimal): Unit = throw UnsupportedOperationException("Update balance not supported")
}
