package org.yechan.remittance.member.internal.contract

fun interface MemberInternalApi {
    fun verify(request: LoginVerifyRequest): LoginVerifyResponse
}
