package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.member.LoginVerifyRequest
import org.yechan.remittance.member.LoginVerifyResponse
import org.yechan.remittance.member.MemberInternalApi
import java.util.concurrent.atomic.AtomicReference

class MemberAuthClientAdapterTest {
    @Test
    fun `회원 인증 클라이언트는 member internal api로 위임한다`() {
        val captured = AtomicReference<LoginVerifyRequest>()
        val memberInternalApi = MemberInternalApi { request ->
            captured.set(request)
            LoginVerifyResponse(true, 7L)
        }
        val adapter = MemberAuthClientAdapter(memberInternalApi)

        val result = adapter.verify("user@example.com", "secret")

        assertThat(result.valid).isTrue()
        assertThat(result.memberId).isEqualTo(7L)
        assertThat(captured.get().email).isEqualTo("user@example.com")
        assertThat(captured.get().password).isEqualTo("secret")
    }
}
