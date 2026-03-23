package org.yechan.remittance.transfer.dto

import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferFailureCode
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferRequestProps
import java.math.BigDecimal
import java.math.RoundingMode

class TransferRequest(
    fromAccountId: Long?,
    toAccountId: Long?,
    amount: BigDecimal?,
) : TransferRequestProps {
    private val fromAccountIdRaw = fromAccountId
    private val toAccountIdRaw = toAccountId
    private val amountRaw = amount

    init {
        if (amountRaw == null || amountRaw <= BigDecimal.ZERO) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Invalid amount")
        }
        if (fromAccountIdRaw == null || toAccountIdRaw == null) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Account IDs must not be null")
        }
    }

    val requestFromAccountId: Long?
        get() = fromAccountIdRaw

    val requestToAccountId: Long?
        get() = toAccountIdRaw

    override val fromAccountId: Long
        get() = requireNotNull(fromAccountIdRaw)

    override val toAccountId: Long
        get() = requireNotNull(toAccountIdRaw)

    override val amount: BigDecimal
        get() = requireNotNull(amountRaw)

    override val scope: TransferProps.TransferScopeValue
        get() = TransferProps.TransferScopeValue.TRANSFER

    override val fee: BigDecimal
        get() = amount.multiply(FEE_RATE).setScale(2, RoundingMode.DOWN)

    private companion object {
        val FEE_RATE = BigDecimal("0.01")
    }
}
