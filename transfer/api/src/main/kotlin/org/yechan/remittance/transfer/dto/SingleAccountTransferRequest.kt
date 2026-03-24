package org.yechan.remittance.transfer.dto

import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferFailureCode
import org.yechan.remittance.transfer.TransferRequestProps
import java.math.BigDecimal

abstract class SingleAccountTransferRequest(
    private val accountIdRaw: Long?,
    private val amountRaw: BigDecimal?,
) : TransferRequestProps {
    init {
        if (amountRaw == null || amountRaw <= BigDecimal.ZERO) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Invalid amount")
        }
        if (accountIdRaw == null) {
            throw TransferFailedException(
                TransferFailureCode.INVALID_REQUEST,
                "Account ID must not be null",
            )
        }
    }

    val accountId: Long?
        get() = accountIdRaw

    override val fromAccountId: Long
        get() = requireNotNull(accountIdRaw)

    override val toAccountId: Long
        get() = requireNotNull(accountIdRaw)

    override val amount: BigDecimal
        get() = requireNotNull(amountRaw)

    override val fee: BigDecimal
        get() = BigDecimal.ZERO
}
