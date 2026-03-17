package org.yechan.remittance.account

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(AccountBeanRegistrar::class, TransferNotificationBeanConfiguration::class)
@AutoConfiguration
class AccountAutoConfiguration

class AccountBeanRegistrar : BeanRegistrarDsl({
    registerBean<AccountCreateUseCase> {
        AccountService(bean())
    }

    registerBean<AccountDeleteUseCase>{
        AccountDeleteService(bean())
    }
})

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(NotificationPushPort::class)
@Import(TransferNotificationBeanRegistrar::class)
class TransferNotificationBeanConfiguration

class TransferNotificationBeanRegistrar : BeanRegistrarDsl({
    registerBean<TransferNotificationUseCase>() {
        TransferNotificationService(
            bean(),
            bean(),
            bean()
        )
    }
})
