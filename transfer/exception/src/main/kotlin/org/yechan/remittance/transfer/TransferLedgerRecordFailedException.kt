package org.yechan.remittance.transfer

import org.yechan.remittance.Status

class TransferLedgerRecordFailedException(
    message: String,
    cause: Throwable,
) : TransferException(Status.INTERNAL_SERVER_ERROR, message) {
    init {
        initCause(cause)
    }
}
