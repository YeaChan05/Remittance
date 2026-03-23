package org.yechan.remittance.member.internal.adapter

import org.yechan.remittance.member.MemberAuthQueryUseCase
import org.yechan.remittance.member.MemberLoginProps
import org.yechan.remittance.member.internal.contract.LoginVerifyRequest
import org.yechan.remittance.member.internal.contract.LoginVerifyResponse
import org.yechan.remittance.member.internal.contract.MemberInternalApi

class MemberInternalAdapter(
    private val memberAuthQueryUseCase: MemberAuthQueryUseCase,
) : MemberInternalApi {
    override fun verify(request: LoginVerifyRequest): LoginVerifyResponse {
        val result = memberAuthQueryUseCase.verify(
            InternalMemberLoginRequest(request.email, request.password),
        )
        return LoginVerifyResponse(result.valid, result.memberId)
    }

    private data class InternalMemberLoginRequest(
        override val email: String,
        override val password: String,
    ) : MemberLoginProps
}
