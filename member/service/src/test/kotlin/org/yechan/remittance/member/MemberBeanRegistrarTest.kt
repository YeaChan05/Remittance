package org.yechan.remittance.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.yechan.remittance.PasswordHashEncoder
import org.yechan.remittance.TokenGenerator

class MemberBeanRegistrarTest {
    @Test
    fun `자동 설정은 member use case 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("memberRepository", mock(MemberRepository::class.java))
            beanFactory.registerSingleton("passwordHashEncoder", mock(PasswordHashEncoder::class.java))
            beanFactory.registerSingleton("tokenGenerator", mock(TokenGenerator::class.java))
            register(TestConfiguration::class.java)
            refresh()
        }

        assertThat(context.getBean(MemberCreateUseCase::class.java)).isInstanceOf(MemberService::class.java)
        assertThat(context.getBean(MemberQueryUseCase::class.java)).isInstanceOf(MemberQueryService::class.java)
        assertThat(context.getBean(MemberAuthenticationQueryUseCase::class.java)).isInstanceOf(MemberAuthenticationQueryService::class.java)
        assertThat(context.getBean(MemberExistenceQueryUseCase::class.java)).isInstanceOf(MemberExistenceQueryService::class.java)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(MemberBeanRegistrar::class)
    class TestConfiguration
}
