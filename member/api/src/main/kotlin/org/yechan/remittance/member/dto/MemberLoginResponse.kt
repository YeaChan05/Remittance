package org.yechan.remittance.member.dto

data class MemberLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
) {
    companion object
}
