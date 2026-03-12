package org.yechan.remittance.auth

import org.yechan.remittance.Status

class AuthInvalidCredentialException(
    message: String
) : AuthException(Status.AUTHENTICATION_FAILED, message)
