package org.yechan.remittance.transfer

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import org.yechan.remittance.whenPropertyEnabled

@Import(TransferOutboxBeanRegistrar::class)
@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(TransferOutboxProperties::class, TransferEventPublisherProperties::class)
class TransferOutboxAutoConfiguration

class TransferOutboxBeanRegistrar : BeanRegistrarDsl({
    whenPropertyEnabled("transfer.outbox", "enabled", matchIfMissing = true) {
        registerBean<TransferEventPublisher> {
            TransferEventPublisherImpl(
                bean<RabbitTemplate>(),
                bean<TransferEventPublisherProperties>()
            )
        }

        registerBean<TransferOutboxPublisher> {
            TransferOutboxPublisher(
                bean(),
                bean<TransferOutboxProperties>()
            )
        }
    }
})
