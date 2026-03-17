package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.yechan.remittance.member.LoginVerifyResponse
import org.yechan.remittance.member.MemberInternalApi

class AuthClientConfigurationTest {
    @Test
    fun `자동 설정은 회원 인증 어댑터를 등록한다`() {
        val memberInternalApi = MemberInternalApi { LoginVerifyResponse(true, 3L) }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("memberInternalApi", memberInternalApi)
            register(AuthClientConfiguration::class.java)
            refresh()
        }

        val client = context.getBean(MemberAuthClient::class.java)

        assertThat(client).isInstanceOf(MemberAuthClientAdapter::class.java)
        assertThat(client.verify("user@example.com", "password").memberId).isEqualTo(3L)

        context.close()
    }
}
