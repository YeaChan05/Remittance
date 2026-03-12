package org.yechan.remittance.transfer

import org.yechan.remittance.Status

class TransferFailedException(
    val failureCode: TransferFailureCode,
    message: String
) : TransferException(Status.BAD_REQUEST, message)
