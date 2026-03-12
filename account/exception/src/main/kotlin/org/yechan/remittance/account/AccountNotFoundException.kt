package org.yechan.remittance.account

import org.yechan.remittance.Status

class AccountNotFoundException(
    message: String
) : AccountException(Status.RESOURCE_NOT_FOUND, message)
