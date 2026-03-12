package org.yechan.remittance.auth

import org.yechan.remittance.BusinessException
import org.yechan.remittance.Status

open class AuthException(
    status: Status,
    message: String
) : BusinessException(status, message)
