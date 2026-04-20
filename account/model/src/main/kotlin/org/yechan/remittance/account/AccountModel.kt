package org.yechan.remittance.account

import org.yechan.remittance.Money

interface AccountModel :
    AccountProps,
    AccountIdentifier {
    fun updateBalance(balance: Money): Unit = throw UnsupportedOperationException("Update balance not supported")
}
