package org.yechan.remittance.auth

data class AuthLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
