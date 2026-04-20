package org.yechan.remittance.account

import org.yechan.remittance.Money
import java.time.LocalDateTime

interface TransferNotificationProps {
    val eventId: Long
    val transferId: Long
    val toAccountId: Long
    val fromAccountId: Long
    val amount: Money
    val occurredAt: LocalDateTime
}
