package org.yechan.remittance.member.internal.contract

data class MemberAuthenticationRequest(
    val email: String,
    val password: String,
)
