package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime

interface TransferRepository {
    fun save(props: TransferRequestProps): TransferModel

    fun findById(identifier: TransferIdentifier): TransferModel?

    fun findCompletedByAccountId(
        identifier: TransferAccountIdentifier,
        condition: TransferQueryCondition,
    ): List<TransferModel>

    fun sumAmountByFromAccountIdAndScopeBetween(
        identifier: TransferAccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        from: LocalDateTime,
        to: LocalDateTime,
    ): BigDecimal
}
