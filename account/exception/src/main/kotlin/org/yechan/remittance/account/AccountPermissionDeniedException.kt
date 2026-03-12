package org.yechan.remittance.account

import org.yechan.remittance.Status

class AccountPermissionDeniedException(
    message: String
) : AccountException(Status.AUTHENTICATION_FAILED, message)
