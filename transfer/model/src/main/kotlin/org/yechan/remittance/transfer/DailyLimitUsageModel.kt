package org.yechan.remittance.transfer

import org.yechan.remittance.Money

interface DailyLimitUsageModel :
    DailyLimitUsageProps,
    DailyLimitUsageIdentifier {
    fun updateUsedAmount(usedAmount: Money): Unit = throw UnsupportedOperationException("Update used amount not supported")
}
