package org.yechan.remittance.transfer

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import java.time.Clock

@EnableConfigurationProperties(IdempotencyKeyProperties::class)
class IdempotencyKeyAutoConfiguration

@AutoConfiguration
class IdempotencyKeyBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<IdempotencyKeyCreateUseCase> {
            IdempotencyKeyService(
                bean(),
                bean<Clock>(),
                bean<IdempotencyKeyProperties>().expiresIn,
            )
        }
    })
