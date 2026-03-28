package org.yechan.remittance.member.internal.adapter

import org.yechan.remittance.member.MemberAuthenticationQueryUseCase
import org.yechan.remittance.member.MemberLoginProps
import org.yechan.remittance.member.internal.contract.MemberAuthenticationInternalApi
import org.yechan.remittance.member.internal.contract.MemberAuthenticationRequest
import org.yechan.remittance.member.internal.contract.MemberAuthenticationResponse

class MemberAuthenticationInternalAdapter(
    private val memberAuthenticationQueryUseCase: MemberAuthenticationQueryUseCase,
) : MemberAuthenticationInternalApi {
    override fun verify(request: MemberAuthenticationRequest): MemberAuthenticationResponse {
        val result = memberAuthenticationQueryUseCase.verify(
            InternalMemberLoginRequest(request.email, request.password),
        )
        return MemberAuthenticationResponse(result.valid, result.memberId)
    }

    private data class InternalMemberLoginRequest(
        override val email: String,
        override val password: String,
    ) : MemberLoginProps
}
