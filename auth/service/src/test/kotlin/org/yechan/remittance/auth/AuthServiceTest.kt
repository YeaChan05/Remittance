package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.yechan.remittance.AuthTokenValue
import org.yechan.remittance.TokenGenerator

class AuthServiceTest {
    @Test
    fun `올바른 인증 정보는 인증 토큰을 반환한다`() {
        val memberAuthClient = MemberAuthClient { _, _ -> MemberAuthResult(true, 1L) }
        val tokenGenerator = TokenGenerator { AuthTokenValue("access", "refresh", 3600L) }
        val useCase: AuthLoginUseCase = AuthService(memberAuthClient, tokenGenerator)

        val token = useCase.login(TestAuthLoginProps())

        assertThat(token.accessToken).isEqualTo("access")
        assertThat(token.refreshToken).isEqualTo("refresh")
        assertThat(token.expiresIn).isEqualTo(3600L)
    }

    @Test
    fun `올바르지 않은 인증 정보는 인증 예외를 던진다`() {
        val memberAuthClient = MemberAuthClient { _, _ -> MemberAuthResult(false, 1L) }
        val tokenGenerator = TokenGenerator { AuthTokenValue("access", "refresh", 3600L) }
        val useCase: AuthLoginUseCase = AuthService(memberAuthClient, tokenGenerator)

        assertThatThrownBy { useCase.login(TestAuthLoginProps()) }
            .isInstanceOf(AuthInvalidCredentialException::class.java)
            .hasMessage("Invalid credentials")
    }

    private class TestAuthLoginProps : AuthLoginProps {
        override val email: String = "user@example.com"
        override val password: String = "password!1"
    }
}
