package org.yechan.remittance.transfer

import java.time.Clock
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(IdempotencyKeyProperties::class)
class IdempotencyKeyAutoConfiguration {
    @Bean
    fun idempotencyKeyCreateUseCase(
        repository: IdempotencyKeyRepository,
        idempotencyKeyClock: Clock,
        properties: IdempotencyKeyProperties
    ): IdempotencyKeyCreateUseCase {
        return IdempotencyKeyService(repository, idempotencyKeyClock, properties.expiresIn)
    }
}
