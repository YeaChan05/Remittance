package org.yechan.remittance.account.internal.adapter

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.yechan.remittance.account.internal.contract.AccountInternalApi

@Import(AccountInternalApiBeanRegistrar::class)
@AutoConfiguration
class AccountInternalApiAutoConfiguration

class AccountInternalApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AccountInternalApi> {
            AccountInternalAdapter(
                bean(),
                bean(),
            )
        }
    })
