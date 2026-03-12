package org.yechan.remittance.transfer

import java.math.BigDecimal
import java.time.LocalDateTime

interface TransferProps {
    val fromAccountId: Long
    val toAccountId: Long
    val amount: BigDecimal
    val scope: TransferScopeValue
    val status: TransferStatusValue
    val requestedAt: LocalDateTime
    val completedAt: LocalDateTime?

    enum class TransferScopeValue {
        DEPOSIT,
        WITHDRAW,
        TRANSFER;

        fun toIdempotencyScope(): IdempotencyKeyProps.IdempotencyScopeValue {
            return when (this) {
                TRANSFER -> IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
                WITHDRAW -> IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW
                DEPOSIT -> IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT
            }
        }
    }

    enum class TransferStatusValue {
        IN_PROGRESS,
        SUCCEEDED,
        FAILED
    }
}
