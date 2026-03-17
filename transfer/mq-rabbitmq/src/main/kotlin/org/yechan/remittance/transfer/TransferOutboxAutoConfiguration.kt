package org.yechan.remittance.transfer

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@Import(TransferOutboxBeanRegistrar::class, TransferOutboxPublisherBeanConfiguration::class)
@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(prefix = "transfer.outbox", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TransferOutboxProperties::class, TransferEventPublisherProperties::class)
class TransferOutboxAutoConfiguration

class TransferOutboxBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferEventPublisher> {
        TransferEventPublisherImpl(
            bean<RabbitTemplate>(),
            bean<TransferEventPublisherProperties>()
        )
    }
})

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(TransferEventPublishUseCase::class)
@Import(TransferOutboxPublisherBeanRegistrar::class)
class TransferOutboxPublisherBeanConfiguration

class TransferOutboxPublisherBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferOutboxPublisher> {
        TransferOutboxPublisher(
            bean(),
            bean<TransferOutboxProperties>()
        )
    }
})
