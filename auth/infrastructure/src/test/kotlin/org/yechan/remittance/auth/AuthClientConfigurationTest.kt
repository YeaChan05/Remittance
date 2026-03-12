package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.member.LoginVerifyResponse
import org.yechan.remittance.member.MemberInternalApi

class AuthClientConfigurationTest {
    @Test
    fun `클라이언트 설정은 회원 인증 어댑터를 노출한다`() {
        val memberInternalApi = MemberInternalApi { LoginVerifyResponse(true, 3L) }
        val configuration = AuthClientConfiguration()

        val client = configuration.memberAuthClient(memberInternalApi)

        assertThat(client).isInstanceOf(MemberAuthClientAdapter::class.java)
        assertThat(client.verify("user@example.com", "password").memberId).isEqualTo(3L)
    }
}
