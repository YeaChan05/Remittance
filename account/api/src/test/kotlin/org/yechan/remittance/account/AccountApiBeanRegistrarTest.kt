package org.yechan.remittance.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

class AccountApiBeanRegistrarTest {
    @Test
    fun `자동 설정은 account api helper 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(TestConfiguration::class.java)
            refresh()
        }

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
    @Import(AccountApiBeanRegistrar::class)
    class TestConfiguration
}
