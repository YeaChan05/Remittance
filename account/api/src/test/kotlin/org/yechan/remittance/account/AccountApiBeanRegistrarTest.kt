package org.yechan.remittance.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

class AccountApiBeanRegistrarTest {
    @Test
    fun `자동 설정은 account api 관련 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton(
                "accountCreateUseCase",
                mock(AccountCreateUseCase::class.java),
            )
            beanFactory.registerSingleton(
                "accountDeleteUseCase",
                mock(AccountDeleteUseCase::class.java),
            )
            register(TestConfiguration::class.java)
            refresh()
        }

        assertThat(context.getBean(AccountController::class.java)).isNotNull
        assertThat(context.getBean(NotificationApiController::class.java)).isNotNull
        val registry = context.getBean(NotificationSessionRegistry::class.java)
        assertThat(registry).isNotNull
        assertThat(context.getBeansOfType(NotificationSubscriptionHandler::class.java)).hasSize(1)
        assertThat(context.getBean(NotificationSubscriptionHandler::class.java)).isSameAs(registry)
        assertThat(context.getBean(NotificationPushPort::class.java)).isInstanceOf(
            NotificationPushAdapter::class.java,
        )

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AccountApiRegistrar::class, AccountApiBeanRegistrar::class)
    class TestConfiguration
}
