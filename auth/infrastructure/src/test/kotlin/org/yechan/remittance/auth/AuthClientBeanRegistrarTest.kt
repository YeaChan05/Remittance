package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.yechan.remittance.member.internal.contract.LoginVerifyResponse
import org.yechan.remittance.member.internal.contract.MemberInternalApi

class AuthClientBeanRegistrarTest {
    @Test
    fun `자동 설정은 회원 인증 어댑터를 등록한다`() {
        val memberInternalApi = MemberInternalApi { LoginVerifyResponse(true, 3L) }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("memberInternalApi", memberInternalApi)
            register(TestConfiguration::class.java)
            refresh()
        }

        val client = context.getBean(MemberAuthClient::class.java)

        assertThat(client).isInstanceOf(MemberAuthClientAdapter::class.java)
        assertThat(client.verify("user@example.com", "password").memberId).isEqualTo(3L)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AuthClientBeanRegistrar::class)
    class TestConfiguration
}
