package org.yechan.remittance.account

import org.yechan.remittance.Money
import java.time.LocalDateTime

class TransferNotificationPayloadParser {
    fun parse(
        eventId: Long,
        payload: String,
    ): TransferNotificationProps {
        val values = FIELD_PATTERN.findAll(payload).associate { matchResult ->
            val key = matchResult.groupValues[1]
            val rawValue = matchResult.groupValues[2]
            val value =
                if (rawValue.startsWith("\"")) {
                    rawValue.substring(1, rawValue.length - 1)
                } else {
                    rawValue
                }
            key to value
        }
        return ParsedTransferNotification(
            eventId,
            values.getValue("transferId").toLong(),
            values.getValue("toAccountId").toLong(),
            values.getValue("fromAccountId").toLong(),
            Money.of(values.getValue("amount").toBigDecimal()),
            LocalDateTime.parse(values.getValue("completedAt")),
        )
    }

    private data class ParsedTransferNotification(
        override val eventId: Long,
        override val transferId: Long,
        override val toAccountId: Long,
        override val fromAccountId: Long,
        override val amount: Money,
        override val occurredAt: LocalDateTime,
    ) : TransferNotificationProps

    private companion object {
        val FIELD_PATTERN = Regex("\"(\\w+)\"\\s*:\\s*(\"[^\"]*\"|[-]?[0-9]+(?:\\.[0-9]+)?)")
    }
}
