package org.yechan.remittance.transfer.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.yechan.remittance.Money
import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferFailureCode
import org.yechan.remittance.transfer.TransferProps
import org.yechan.remittance.transfer.TransferRequestProps
import java.math.BigDecimal
import java.math.RoundingMode

class TransferRequest(
    @get:JsonProperty("fromAccountId")
    @param:JsonProperty("fromAccountId")
    val requestFromAccountId: Long?,
    @get:JsonProperty("toAccountId")
    @param:JsonProperty("toAccountId")
    val requestToAccountId: Long?,
    @get:JsonProperty("amount")
    @param:JsonProperty("amount")
    val requestAmount: BigDecimal?,
) : TransferRequestProps {
    init {
        if (requestAmount == null || requestAmount <= BigDecimal.ZERO) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Invalid amount")
        }
        if (requestFromAccountId == null || requestToAccountId == null) {
            throw TransferFailedException(
                TransferFailureCode.INVALID_REQUEST,
                "Account IDs must not be null",
            )
        }
    }

    @get:JsonIgnore
    override val fromAccountId: Long
        get() = requireNotNull(requestFromAccountId)

    @get:JsonIgnore
    override val toAccountId: Long
        get() = requireNotNull(requestToAccountId)

    @get:JsonIgnore
    override val amount: Money
        get() = Money.of(requireNotNull(requestAmount))

    @get:JsonIgnore
    override val scope: TransferProps.TransferScopeValue
        get() = TransferProps.TransferScopeValue.TRANSFER

    @get:JsonIgnore
    override val fee: Money
        get() = amount.multiply(FEE_RATE, RoundingMode.DOWN)

    private companion object {
        val FEE_RATE = BigDecimal("0.01")
    }
}
