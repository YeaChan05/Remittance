package org.yechan.remittance.account

import java.math.BigDecimal
import java.time.LocalDateTime

interface TransferNotificationProps {
    val eventId: Long
    val transferId: Long
    val toAccountId: Long
    val fromAccountId: Long
    val amount: BigDecimal
    val occurredAt: LocalDateTime
}
