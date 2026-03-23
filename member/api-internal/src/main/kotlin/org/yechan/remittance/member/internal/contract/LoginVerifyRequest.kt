package org.yechan.remittance.member.internal.contract

data class LoginVerifyRequest(
    val email: String,
    val password: String,
)
