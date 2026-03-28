package org.yechan.remittance.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MemberLoginControllerTest {
    @Test
    fun `올바른 로그인 요청은 토큰 응답을 반환한다`() {
        val memberQueryUseCase = object : MemberQueryUseCase {
            override fun login(props: MemberLoginProps): MemberTokenValue = MemberTokenValue(
                "access-token",
                "refresh-token",
                1200L,
            )
        }
        val controller = MemberLoginController(memberQueryUseCase)
        val request = org.yechan.remittance.member.dto.MemberLoginRequest("user@example.com", "password!1")

        val response = controller.login(request)

        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body).isNotNull
        assertThat(response.body!!.accessToken).isEqualTo("access-token")
        assertThat(response.body!!.refreshToken).isEqualTo("refresh-token")
        assertThat(response.body!!.expiresIn).isEqualTo(1200L)
    }
}
