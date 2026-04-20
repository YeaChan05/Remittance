package org.yechan.remittance.transfer

import org.yechan.remittance.Money
import java.time.LocalDateTime

data class Transfer(
    override val transferId: Long?,
    override val fromAccountId: Long,
    override val toAccountId: Long,
    override val amount: Money,
    override val scope: TransferProps.TransferScopeValue,
    override val status: TransferProps.TransferStatusValue,
    override val requestedAt: LocalDateTime,
    override val completedAt: LocalDateTime?,
) : TransferModel
