package org.yechan.remittance.member

data class MemberTokenValue(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
