package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import java.time.Clock

@Import(IdempotencyKeyBeanRegistrar::class)
@AutoConfiguration
@EnableConfigurationProperties(IdempotencyKeyProperties::class)
class IdempotencyKeyAutoConfiguration

class IdempotencyKeyBeanRegistrar : BeanRegistrarDsl({
    registerBean<IdempotencyKeyCreateUseCase> {
        IdempotencyKeyService(
            bean(),
            bean<Clock>(),
            bean<IdempotencyKeyProperties>().expiresIn
        )
    }
})
