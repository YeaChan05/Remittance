package org.yechan.remittance.auth

data class MemberAuthResult(
    val valid: Boolean,
    val memberId: Long
)
