package org.yechan.remittance.account

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransferNotificationMessage(
    val type: String,
    val transferId: Long,
    val amount: BigDecimal,
    val fromAccountId: Long,
    val occurredAt: LocalDateTime
)
