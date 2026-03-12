package org.yechan.remittance.transfer

import org.yechan.remittance.BusinessException
import org.yechan.remittance.Status

open class TransferException : BusinessException {
    constructor(message: String) : super(message)

    constructor(status: Status, message: String) : super(status, message)
}
