package org.yechan.remittance.account

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.yechan.remittance.whenPropertyEnabled

@Import(TransferNotificationInfrastructureBeanRegistrar::class)
@AutoConfiguration
@EnableRabbit
@EnableConfigurationProperties(TransferNotificationConsumerProperties::class)
class TransferNotificationConsumerAutoConfiguration

class TransferNotificationInfrastructureBeanRegistrar : BeanRegistrarDsl({
    whenPropertyEnabled("account.transfer-notification", "enabled", matchIfMissing = true) {
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

        registerBean<TransferNotificationConsumer> {
            TransferNotificationConsumer(
                bean(),
                bean()
            )
        }
    }
})
