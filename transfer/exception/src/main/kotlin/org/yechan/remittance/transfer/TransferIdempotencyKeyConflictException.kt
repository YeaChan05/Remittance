package org.yechan.remittance.transfer

import org.yechan.remittance.Status

class TransferIdempotencyKeyConflictException(
    message: String,
) : TransferException(Status.BAD_REQUEST, message)
