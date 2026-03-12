package org.yechan.remittance.transfer

import org.yechan.remittance.account.AccountIdentifier
import java.time.LocalDate

interface DailyLimitUsageRepository {
    fun findOrCreateForUpdate(
        identifier: AccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        usageDate: LocalDate
    ): DailyLimitUsageModel
}
