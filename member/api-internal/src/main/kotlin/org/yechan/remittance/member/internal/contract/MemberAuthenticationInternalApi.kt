package org.yechan.remittance.member.internal.contract

import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange("/internal/members")
fun interface MemberAuthenticationInternalApi {
    @PostExchange("/auth")
    fun verify(@RequestBody request: MemberAuthenticationRequest): MemberAuthenticationResponse
}
