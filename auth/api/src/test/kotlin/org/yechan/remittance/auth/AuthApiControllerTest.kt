package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yechan.remittance.AuthTokenValue

class AuthApiControllerTest {
    @Test
    fun `올바른 로그인 요청은 토큰 응답을 반환한다`() {
        val authLoginUseCase = AuthLoginUseCase {
            AuthTokenValue("access-token", "refresh-token", 1200L)
        }
        val controller = AuthApiController(authLoginUseCase)
        val request = AuthLoginRequest("user@example.com", "password!1")

        val response = controller.login(request)

        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        assertThat(response.body).isNotNull
        assertThat(response.body!!.accessToken).isEqualTo("access-token")
        assertThat(response.body!!.refreshToken).isEqualTo("refresh-token")
        assertThat(response.body!!.expiresIn).isEqualTo(1200L)
    }
}
