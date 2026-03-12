package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDate

interface DailyLimitUsageProps {
    val accountId: Long
    val scope: TransferProps.TransferScopeValue
    val usageDate: LocalDate
    val usedAmount: BigDecimal
}
