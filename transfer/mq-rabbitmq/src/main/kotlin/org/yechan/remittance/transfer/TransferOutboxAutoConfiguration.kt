package org.yechan.remittance.transfer

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(prefix = "transfer.outbox", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(TransferOutboxProperties::class, TransferEventPublisherProperties::class)
class TransferOutboxAutoConfiguration {
    @Bean
    fun transferEventPublisher(
        rabbitTemplate: RabbitTemplate,
        properties: TransferEventPublisherProperties
    ): TransferEventPublisher {
        return TransferEventPublisherImpl(rabbitTemplate, properties)
    }

    @Bean
    @ConditionalOnBean(TransferEventPublishUseCase::class)
    fun transferOutboxPublisher(
        transferEventPublishUseCase: TransferEventPublishUseCase,
        properties: TransferOutboxProperties
    ): TransferOutboxPublisher {
        return TransferOutboxPublisher(transferEventPublishUseCase, properties)
    }
}
