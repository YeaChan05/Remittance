package org.yechan.remittance.account

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@Import(AccountBeanRegistrar::class)
@AutoConfiguration
class AccountAutoConfiguration

class AccountBeanRegistrar : BeanRegistrarDsl({
    registerBean<AccountCreateUseCase> {
        AccountService(bean())
    }

    registerBean<AccountDeleteUseCase>{
        AccountDeleteService(bean())
    }

    registerBean<TransferNotificationUseCase> {
        TransferNotificationService(
            bean(),
            bean(),
            bean()
        )
    }
})
