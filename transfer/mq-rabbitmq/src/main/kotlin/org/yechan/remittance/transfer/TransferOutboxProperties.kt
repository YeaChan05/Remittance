package org.yechan.remittance.transfer

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("transfer.outbox")
class TransferOutboxProperties {
    var batchSize: Int = 100
    var publishDelayMs: Long = 1000
}
