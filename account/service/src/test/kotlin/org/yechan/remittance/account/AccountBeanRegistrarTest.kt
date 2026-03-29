package org.yechan.remittance.account

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

class AccountBeanRegistrarTest {
    @Test
    fun `자동 설정은 account use case 빈을 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            beanFactory.registerSingleton("accountRepository", mock(AccountRepository::class.java))
            beanFactory.registerSingleton(
                "processedEventRepository",
                mock(ProcessedEventRepository::class.java),
            )
            beanFactory.registerSingleton(
                "notificationPushPort",
                mock(NotificationPushPort::class.java),
            )
            register(TestConfiguration::class.java)
            refresh()
        }

        assertThat(context.getBean(AccountCreateUseCase::class.java)).isInstanceOf(AccountService::class.java)
        assertThat(context.getBean(AccountDeleteUseCase::class.java)).isInstanceOf(
            AccountDeleteService::class.java,
        )
        assertThat(context.getBean(AccountInternalQueryUseCase::class.java)).isInstanceOf(
            AccountInternalQueryService::class.java,
        )
        assertThat(context.getBean(AccountInternalUpdateUseCase::class.java)).isInstanceOf(
            AccountInternalUpdateService::class.java,
        )
        assertThat(context.getBean(TransferNotificationUseCase::class.java)).isInstanceOf(
            TransferNotificationService::class.java,
        )

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AccountBeanRegistrar::class)
    class TestConfiguration
}
