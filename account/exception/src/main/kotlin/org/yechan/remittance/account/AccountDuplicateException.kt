package org.yechan.remittance.account

import org.yechan.remittance.Status

class AccountDuplicateException(
    message: String,
) : AccountException(Status.BAD_REQUEST, message)
