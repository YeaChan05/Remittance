package org.yechan.remittance.member.internal.adapter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.yechan.remittance.member.MemberAuthenticationQueryUseCase
import org.yechan.remittance.member.MemberAuthenticationResult
import org.yechan.remittance.member.MemberExistenceQueryUseCase
import org.yechan.remittance.member.internal.contract.MemberAuthenticationRequest

class MemberInternalApiBeanRegistrarTest {
    @Test
    fun `자동 설정은 회원 내부 controller 빈을 등록한다`() {
        val memberAuthQueryUseCase =
            MemberAuthenticationQueryUseCase { MemberAuthenticationResult(true, 3L) }
        val memberExistenceQueryUseCase = MemberExistenceQueryUseCase { it == 3L }
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("memberAuthQueryUseCase", memberAuthQueryUseCase)
            beanFactory.registerSingleton(
                "memberExistenceQueryUseCase",
                memberExistenceQueryUseCase,
            )
            register(TestConfiguration::class.java)
            refresh()
        }

        val memberAuthenticationController =
            context.getBean(MemberAuthenticationInternalController::class.java)
        val memberExistenceController =
            context.getBean(MemberExistenceInternalController::class.java)

        assertThat(
            memberAuthenticationController.verify(
                MemberAuthenticationRequest(
                    "user@example.com",
                    "password",
                ),
            ).memberId,
        ).isEqualTo(3L)
        assertThat(
            memberExistenceController.exists(
                org.yechan.remittance.member.internal.contract.MemberExistsRequest(3L),
            ).exists,
        ).isTrue()

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(
        MemberInternalApiBeanRegistrar::class,
        MemberAuthenticationInternalController::class,
        MemberExistenceInternalController::class,
    )
    class TestConfiguration
}
