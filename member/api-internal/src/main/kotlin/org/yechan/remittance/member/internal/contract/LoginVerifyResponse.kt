package org.yechan.remittance.member.internal.contract

data class LoginVerifyResponse(
    val valid: Boolean,
    val memberId: Long,
)
