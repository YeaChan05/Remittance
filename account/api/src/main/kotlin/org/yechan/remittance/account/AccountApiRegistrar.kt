package org.yechan.remittance.account

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@Import(AccountController::class, NotificationApiController::class)
class AccountApiRegistrar

@AutoConfiguration
class AccountApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<NotificationSessionRegistry> {
            NotificationSessionRegistry()
        }

        registerBean<NotificationPushPort> {
            NotificationPushAdapter(bean())
        }
    })
