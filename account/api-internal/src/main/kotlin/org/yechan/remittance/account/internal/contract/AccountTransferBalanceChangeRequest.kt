package org.yechan.remittance.account.internal.contract

import java.math.BigDecimal

data class AccountTransferBalanceChangeRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val debitAmount: BigDecimal,
    val creditAmount: BigDecimal,
)
