package org.yechan.remittance.auth

fun interface MemberAuthClient {
    fun verify(
        email: String,
        password: String,
    ): MemberAuthResult
}
