package org.yechan.remittance.transfer

import java.math.BigDecimal

interface TransferRequestProps {
    val fromAccountId: Long
    val toAccountId: Long
    val amount: BigDecimal
    val scope: TransferProps.TransferScopeValue
    val fee: BigDecimal

    fun toIdempotencyScope(): IdempotencyKeyProps.IdempotencyScopeValue {
        return when (scope) {
            TransferProps.TransferScopeValue.WITHDRAW ->
                IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW
            TransferProps.TransferScopeValue.DEPOSIT ->
                IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT
            TransferProps.TransferScopeValue.TRANSFER ->
                IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
        }
    }

    fun debit(): BigDecimal {
        return amount.add(fee)
    }
}
