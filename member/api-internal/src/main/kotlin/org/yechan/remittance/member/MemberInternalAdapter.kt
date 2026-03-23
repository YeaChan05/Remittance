package org.yechan.remittance.member

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
