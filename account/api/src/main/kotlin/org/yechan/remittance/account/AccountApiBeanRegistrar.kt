package org.yechan.remittance.account

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration

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
