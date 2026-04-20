package org.yechan.remittance.transfer

import org.yechan.remittance.Money
import java.time.LocalDate

interface DailyLimitUsageProps {
    val accountId: Long
    val scope: TransferProps.TransferScopeValue
    val usageDate: LocalDate
    val usedAmount: Money
}
