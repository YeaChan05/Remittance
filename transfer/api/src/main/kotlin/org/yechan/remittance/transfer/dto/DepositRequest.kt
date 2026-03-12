package org.yechan.remittance.transfer.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferFailureCode
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferRequestProps

class DepositRequest(
    @param:JsonProperty("accountId") private val accountIdRaw: Long?,
    @param:JsonProperty("amount") private val amountRaw: BigDecimal?
) : TransferRequestProps {
    init {
        if (amountRaw == null || amountRaw.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Invalid amount")
        }
        if (accountIdRaw == null) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Account ID must not be null")
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

    override val scope: TransferProps.TransferScopeValue
        get() = TransferProps.TransferScopeValue.DEPOSIT

    override val fee: BigDecimal
        get() = BigDecimal.ZERO
}
