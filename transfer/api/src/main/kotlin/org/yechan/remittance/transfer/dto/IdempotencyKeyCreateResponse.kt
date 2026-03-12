package org.yechan.remittance.transfer.dto

import java.time.LocalDateTime

data class IdempotencyKeyCreateResponse(
    val idempotencyKey: String,
    val expiresAt: LocalDateTime
)
