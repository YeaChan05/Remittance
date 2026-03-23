package org.yechan.remittance.transfer

import java.math.BigDecimal

interface DailyLimitUsageModel :
    DailyLimitUsageProps,
    DailyLimitUsageIdentifier {
    fun updateUsedAmount(usedAmount: BigDecimal): Unit = throw UnsupportedOperationException("Update used amount not supported")
}
