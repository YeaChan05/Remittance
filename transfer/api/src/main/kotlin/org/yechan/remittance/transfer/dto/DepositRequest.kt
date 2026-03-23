package org.yechan.remittance.transfer.dto

import org.yechan.remittance.transfer.TransferProps
import java.math.BigDecimal

class DepositRequest(
    accountId: Long?,
    amount: BigDecimal?,
) : SingleAccountTransferRequest(accountId, amount) {
    override val scope: TransferProps.TransferScopeValue
        get() = TransferProps.TransferScopeValue.DEPOSIT
}
