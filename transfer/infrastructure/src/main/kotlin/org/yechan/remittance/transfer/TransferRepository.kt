package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime
import org.yechan.remittance.account.AccountIdentifier

interface TransferRepository {
    fun save(props: TransferRequestProps): TransferModel

    fun findById(identifier: TransferIdentifier): TransferModel?

    fun findCompletedByAccountId(
        identifier: AccountIdentifier,
        condition: TransferQueryCondition
    ): List<TransferModel>

    fun sumAmountByFromAccountIdAndScopeBetween(
        identifier: AccountIdentifier,
        scope: TransferProps.TransferScopeValue,
        from: LocalDateTime,
        to: LocalDateTime
    ): BigDecimal
}
