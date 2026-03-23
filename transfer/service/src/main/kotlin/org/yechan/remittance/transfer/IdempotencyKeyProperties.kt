package org.yechan.remittance.transfer

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@ConfigurationProperties(prefix = "transfer.idempotency-key")
@Validated
data class IdempotencyKeyProperties(
    val expiresIn: Duration,
)
