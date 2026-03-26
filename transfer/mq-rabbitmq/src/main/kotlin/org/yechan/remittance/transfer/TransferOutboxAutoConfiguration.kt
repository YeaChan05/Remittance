package org.yechan.remittance.transfer

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.core.env.getProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.yechan.remittance.whenPropertyEnabled

@EnableScheduling
@EnableConfigurationProperties(
    TransferOutboxProperties::class,
    TransferEventPublisherProperties::class,
)
class TransferOutboxAutoConfiguration

@AutoConfiguration
class TransferOutboxBeanRegistrar :
    BeanRegistrarDsl({
        whenPropertyEnabled("transfer.outbox", "enabled", matchIfMissing = true) {
            registerBean<TransferEventPublisher> {
                TransferEventPublisherImpl(
                    bean<RabbitTemplate>(),
                    bean<TransferEventPublisherProperties>(),
                )
            }

            whenPropertyEnabled("transfer.outbox.publisher", "enabled", matchIfMissing = true) {
                registerBean<TransferOutboxPublisher> {
                    TransferOutboxPublisher(
                        bean(),
                        bean<TransferOutboxProperties>(),
                    )
                }
            }
        }
    })
