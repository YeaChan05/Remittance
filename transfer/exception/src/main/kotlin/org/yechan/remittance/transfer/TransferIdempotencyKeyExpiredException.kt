package org.yechan.remittance.transfer

import org.yechan.remittance.Status

class TransferIdempotencyKeyExpiredException(
    message: String
) : TransferException(Status.BAD_REQUEST, message)
