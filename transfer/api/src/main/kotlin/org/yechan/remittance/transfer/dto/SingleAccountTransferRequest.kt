package org.yechan.remittance.transfer.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.yechan.remittance.Money
import org.yechan.remittance.transfer.TransferFailedException
import org.yechan.remittance.transfer.TransferFailureCode
import org.yechan.remittance.transfer.TransferRequestProps
import java.math.BigDecimal

abstract class SingleAccountTransferRequest(
    @get:JsonProperty("accountId")
    @param:JsonProperty("accountId")
    val accountId: Long?,
    @get:JsonProperty("amount")
    @param:JsonProperty("amount")
    val requestAmount: BigDecimal?,
) : TransferRequestProps {
    init {
        if (requestAmount == null || requestAmount <= BigDecimal.ZERO) {
            throw TransferFailedException(TransferFailureCode.INVALID_REQUEST, "Invalid amount")
        }
        if (accountId == null) {
            throw TransferFailedException(
                TransferFailureCode.INVALID_REQUEST,
                "Account ID must not be null",
            )
        }
    }

    @get:JsonIgnore
    override val fromAccountId: Long
        get() = requireNotNull(accountId)

    @get:JsonIgnore
    override val toAccountId: Long
        get() = requireNotNull(accountId)

    @get:JsonIgnore
    override val amount: Money
        get() = Money.of(requireNotNull(requestAmount))

    @get:JsonIgnore
    override val fee: Money
        get() = Money.zero()
}
