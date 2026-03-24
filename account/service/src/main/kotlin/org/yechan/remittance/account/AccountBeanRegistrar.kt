package org.yechan.remittance.account

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration
class AccountBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AccountCreateUseCase> {
            AccountService(bean())
        }

        registerBean<AccountDeleteUseCase> {
            AccountDeleteService(bean())
        }

        registerBean<AccountInternalQueryUseCase> {
            AccountInternalQueryService(bean())
        }

        registerBean<AccountInternalUpdateUseCase> {
            AccountInternalUpdateService(bean())
        }

        registerBean<TransferNotificationUseCase> {
            TransferNotificationService(
                bean(),
                bean(),
                bean(),
            )
        }
    })
