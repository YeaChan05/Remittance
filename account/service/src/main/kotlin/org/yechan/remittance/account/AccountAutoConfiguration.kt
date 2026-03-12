package org.yechan.remittance.account

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean

@AutoConfiguration
class AccountAutoConfiguration {
    @Bean
    fun accountCreateUseCase(accountRepository: AccountRepository): AccountCreateUseCase {
        return AccountService(accountRepository)
    }

    @Bean
    fun accountDeleteUseCase(accountRepository: AccountRepository): AccountDeleteUseCase {
        return AccountDeleteService(accountRepository)
    }

    @Bean
    @ConditionalOnBean(NotificationPushPort::class)
    fun transferNotificationUseCase(
        accountRepository: AccountRepository,
        processedEventRepository: ProcessedEventRepository,
        notificationPushPort: NotificationPushPort
    ): TransferNotificationUseCase {
        return TransferNotificationService(
            accountRepository,
            processedEventRepository,
            notificationPushPort
        )
    }
}
