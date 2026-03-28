package org.yechan.remittance.member

data class MemberAuthenticationResult(
    val valid: Boolean,
    val memberId: Long,
)
