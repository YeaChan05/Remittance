package org.yechan.remittance.transfer

import org.yechan.remittance.Money
import org.yechan.remittance.transfer.TransferProps.TransferScopeValue

interface TransferRequestProps {
    val fromAccountId: Long
    val toAccountId: Long
    val amount: Money
    val scope: TransferScopeValue
    val fee: Money

    fun toIdempotencyScope(): IdempotencyKeyProps.IdempotencyScopeValue = when (scope) {
        TransferScopeValue.WITHDRAW ->
            IdempotencyKeyProps.IdempotencyScopeValue.WITHDRAW

        TransferScopeValue.DEPOSIT ->
            IdempotencyKeyProps.IdempotencyScopeValue.DEPOSIT

        TransferScopeValue.TRANSFER ->
            IdempotencyKeyProps.IdempotencyScopeValue.TRANSFER
    }

    fun debit(): Money = amount.add(fee)

    fun isValid(): Boolean = when (scope) {
        TransferScopeValue.WITHDRAW, TransferScopeValue.TRANSFER ->
            fromAccountId != toAccountId && amount.isPositive()

        TransferScopeValue.DEPOSIT ->
            fromAccountId != toAccountId && amount.isPositive() && fee.isZero()
    }
}
