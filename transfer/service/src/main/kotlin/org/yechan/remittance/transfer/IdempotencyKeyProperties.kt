package org.yechan.remittance.transfer

import java.time.Duration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "transfer.idempotency-key")
@Validated
data class IdempotencyKeyProperties(
    val expiresIn: Duration
)
