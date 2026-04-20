package org.yechan.remittance.transfer

import org.yechan.remittance.Money

interface TransferRequestProps {
    val fromAccountId: Long
    val toAccountId: Long
    val amount: Money
    val scope: TransferProps.TransferScopeValue
    val fee: Money

    fun toIdempotencyScope(): IdempotencyKeyProps.IdempotencyScopeValue = when (scope) {
        TransferProps.TransferScopeValue.WITHDRAW ->
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW

        TransferProps.TransferScopeValue.DEPOSIT ->
            IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT

        TransferProps.TransferScopeValue.TRANSFER ->
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
    }

    fun debit(): Money = amount.add(fee)
}
