package org.yechan.remittance.account

import org.yechan.remittance.BusinessException
import org.yechan.remittance.Status

open class AccountException : BusinessException {
    constructor(message: String) : super(message)

    constructor(
        status: Status,
        message: String,
    ) : super(status, message)
}
