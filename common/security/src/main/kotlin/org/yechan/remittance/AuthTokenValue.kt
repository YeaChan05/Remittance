package org.yechan.remittance

data class AuthTokenValue(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
