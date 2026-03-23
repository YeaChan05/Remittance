package org.yechan.remittance.transfer

import java.time.LocalDateTime

data class TransferQueryCondition(
    val from: LocalDateTime?,
    val to: LocalDateTime?,
    val limit: Int?,
)
