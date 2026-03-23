package org.yechan.remittance.account.internal.contract

import java.math.BigDecimal

data class AccountBalanceChangeRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal,
)
