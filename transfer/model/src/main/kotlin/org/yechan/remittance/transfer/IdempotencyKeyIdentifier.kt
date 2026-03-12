package org.yechan.remittance.transfer

interface IdempotencyKeyIdentifier {
    val idempotencyKeyId: Long?
}
