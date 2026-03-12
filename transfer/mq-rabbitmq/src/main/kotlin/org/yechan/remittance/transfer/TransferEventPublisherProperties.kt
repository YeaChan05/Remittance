package org.yechan.remittance.transfer

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("transfer.outbox.publisher")
class TransferEventPublisherProperties {
    var exchange: String = "transfer.exchange"
    var routingKey: String = "transfer.completed"
}
