package org.yechan.remittance.account

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("account.transfer-notification")
class TransferNotificationConsumerProperties {
    var queue: String = "transfer.completed.queue"
    var exchange: String = "transfer.exchange"
    var routingKey: String = "transfer.completed"
}
