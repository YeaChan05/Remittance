package org.yechan.remittance.member.internal.contract

fun interface MemberAuthenticationInternalApi {
    fun verify(request: MemberAuthenticationRequest): MemberAuthenticationResponse
}
