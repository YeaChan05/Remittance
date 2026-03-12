package org.yechan.remittance.account

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@AutoConfiguration
@Import(AccountController::class, NotificationApiController::class)
class AccountApiAutoConfiguration {
    @Bean
    fun notificationSessionRegistry(): NotificationSessionRegistry {
        return NotificationSessionRegistry()
    }

    @Bean
    fun notificationPushPort(registry: NotificationSessionRegistry): NotificationPushPort {
        return NotificationPushAdapter(registry)
    }
}
