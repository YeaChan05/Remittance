package org.yechan.remittance.member

data class LoginVerifyRequest(
    val email: String,
    val password: String,
)
