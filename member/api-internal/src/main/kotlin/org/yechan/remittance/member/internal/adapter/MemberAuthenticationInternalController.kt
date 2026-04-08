package org.yechan.remittance.member.internal.adapter

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.yechan.remittance.member.MemberAuthenticationQueryUseCase
import org.yechan.remittance.member.MemberLoginProps
import org.yechan.remittance.member.internal.contract.MemberAuthenticationRequest
import org.yechan.remittance.member.internal.contract.MemberAuthenticationResponse

@RestController
@RequestMapping("/internal/members")
class MemberAuthenticationInternalController(
    private val memberAuthenticationQueryUseCase: MemberAuthenticationQueryUseCase,
) {
    @PostMapping("/auth")
    fun verify(
        @RequestBody request: MemberAuthenticationRequest,
    ): MemberAuthenticationResponse {
        val result = memberAuthenticationQueryUseCase.verify(
            InternalMemberLoginRequest(
                request.email,
                request.password,
            ),
        )
        return MemberAuthenticationResponse(result.valid, result.memberId)
    }

    private data class InternalMemberLoginRequest(
        override val email: String,
        override val password: String,
    ) : MemberLoginProps
}
