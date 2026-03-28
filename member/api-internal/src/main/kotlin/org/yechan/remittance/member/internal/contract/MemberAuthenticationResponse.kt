package org.yechan.remittance.member.internal.contract

data class MemberAuthenticationResponse(
    val valid: Boolean,
    val memberId: Long,
)
