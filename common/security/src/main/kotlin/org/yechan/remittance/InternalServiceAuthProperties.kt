package org.yechan.remittance

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "auth.internal")
@Validated
data class InternalServiceAuthProperties(
    @field:NotBlank
    val token: String,
)
