package org.yechan.remittance.member

data class LoginVerifyResponse(
    val valid: Boolean,
    val memberId: Long
)
