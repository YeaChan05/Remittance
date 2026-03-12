package org.yechan.remittance.member

fun interface MemberInternalApi {
    fun verify(request: LoginVerifyRequest): LoginVerifyResponse
}
