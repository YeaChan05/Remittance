package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime

data class Transfer(
    override val transferId: Long?,
    override val fromAccountId: Long,
    override val toAccountId: Long,
    override val amount: BigDecimal,
    override val scope: TransferProps.TransferScopeValue,
    override val status: TransferProps.TransferStatusValue,
    override val requestedAt: LocalDateTime,
    override val completedAt: LocalDateTime?
) : TransferModel
