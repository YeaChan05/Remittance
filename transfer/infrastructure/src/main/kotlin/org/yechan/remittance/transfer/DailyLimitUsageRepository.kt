package org.yechan.remittance.transfer

import java.time.LocalDate

interface DailyLimitUsageRepository {
    fun findOrCreateForUpdate(
        identifier: TransferAccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        usageDate: LocalDate,
    ): DailyLimitUsageModel
}
