package org.yechan.remittance.account

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableRabbit
@ConditionalOnProperty(
    prefix = "account.transfer-notification",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(TransferNotificationConsumerProperties::class)
class TransferNotificationConsumerAutoConfiguration {
    @Bean
    fun transferNotificationPayloadParser(): TransferNotificationPayloadParser {
        return TransferNotificationPayloadParser()
    }

    @Bean
    fun transferNotificationExchange(properties: TransferNotificationConsumerProperties): DirectExchange {
        return DirectExchange(properties.exchange)
    }

    @Bean
    fun transferNotificationQueue(properties: TransferNotificationConsumerProperties): Queue {
        return Queue(properties.queue)
    }

    @Bean
    fun transferNotificationBinding(
        transferNotificationQueue: Queue,
        transferNotificationExchange: DirectExchange,
        properties: TransferNotificationConsumerProperties
    ): Binding {
        return BindingBuilder.bind(transferNotificationQueue)
            .to(transferNotificationExchange)
            .with(properties.routingKey)
    }

    @Bean
    @ConditionalOnBean(TransferNotificationUseCase::class)
    fun transferNotificationConsumer(
        transferNotificationUseCase: TransferNotificationUseCase,
        parser: TransferNotificationPayloadParser
    ): TransferNotificationConsumer {
        return TransferNotificationConsumer(transferNotificationUseCase, parser)
    }
}
