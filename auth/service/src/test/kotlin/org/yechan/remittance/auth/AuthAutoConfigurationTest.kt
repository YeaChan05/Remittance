package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.yechan.remittance.AuthTokenValue
import org.yechan.remittance.TokenGenerator

class AuthAutoConfigurationTest {
    @Test
    fun `자동 설정은 인증 로그인 유스케이스 빈을 등록한다`() {
        val memberAuthClient = MemberAuthClient { _, _ -> MemberAuthResult(true, 2L) }
        val tokenGenerator = TokenGenerator { AuthTokenValue("token", "refresh", 100L) }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("memberAuthClient", memberAuthClient)
            beanFactory.registerSingleton("tokenGenerator", tokenGenerator)
            register(AuthAutoConfiguration::class.java)
            refresh()
        }

        val useCase = context.getBean(AuthLoginUseCase::class.java)
        val token = useCase.login(
            object : AuthLoginProps {
                override val email: String = "user@example.com"
                override val password: String = "password!1"
            },
        )

        assertThat(token.accessToken).isEqualTo("token")
        assertThat(token.refreshToken).isEqualTo("refresh")
        assertThat(token.expiresIn).isEqualTo(100L)

        context.close()
    }
}
