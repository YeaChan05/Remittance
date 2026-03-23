package org.yechan.remittance.transfer

import org.yechan.remittance.Status

class TransferIdempotencyKeyNotFoundException(
    message: String,
) : TransferException(Status.RESOURCE_NOT_FOUND, message)
