package org.yechan.remittance.auth

import org.yechan.remittance.member.LoginVerifyRequest
import org.yechan.remittance.member.MemberInternalApi

class MemberAuthClientAdapter(
    private val memberInternalApi: MemberInternalApi
) : MemberAuthClient {
    override fun verify(
        email: String,
        password: String
    ): MemberAuthResult {
        val response = memberInternalApi.verify(LoginVerifyRequest(email, password))
        return MemberAuthResult(response.valid, response.memberId)
    }
}
