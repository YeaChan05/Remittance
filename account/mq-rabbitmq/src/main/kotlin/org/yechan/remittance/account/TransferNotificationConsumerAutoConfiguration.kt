package org.yechan.remittance.account

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(
    TransferNotificationInfrastructureBeanRegistrar::class,
    TransferNotificationConsumerBeanConfiguration::class
)
@AutoConfiguration
@EnableRabbit
@ConditionalOnProperty(
    prefix = "account.transfer-notification",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(TransferNotificationConsumerProperties::class)
class TransferNotificationConsumerAutoConfiguration

class TransferNotificationInfrastructureBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferNotificationPayloadParser> {
        TransferNotificationPayloadParser()
    }

    registerBean<DirectExchange> {
        DirectExchange(bean<TransferNotificationConsumerProperties>().exchange)
    }

    registerBean<Queue> {
        Queue(bean<TransferNotificationConsumerProperties>().queue)
    }

    registerBean<Binding> {
        BindingBuilder.bind(bean<Queue>())
            .to(bean<DirectExchange>())
            .with(bean<TransferNotificationConsumerProperties>().routingKey)
    }
})

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(TransferNotificationUseCase::class)
@Import(TransferNotificationConsumerBeanRegistrar::class)
class TransferNotificationConsumerBeanConfiguration

class TransferNotificationConsumerBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferNotificationConsumer> {
        TransferNotificationConsumer(
            bean(),
            bean()
        )
    }
})