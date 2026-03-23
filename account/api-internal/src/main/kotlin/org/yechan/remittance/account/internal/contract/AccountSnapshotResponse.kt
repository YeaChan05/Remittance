package org.yechan.remittance.account.internal.contract

import java.math.BigDecimal

data class AccountSnapshotResponse(
    val accountId: Long,
    val memberId: Long,
    val balance: BigDecimal,
)
