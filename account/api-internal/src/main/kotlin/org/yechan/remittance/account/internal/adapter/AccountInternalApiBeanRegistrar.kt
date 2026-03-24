package org.yechan.remittance.account.internal.adapter

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.yechan.remittance.account.internal.contract.AccountInternalApi

@AutoConfiguration
class AccountInternalApiBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AccountInternalApi> {
            AccountInternalAdapter(
                bean(),
                bean(),
            )
        }
    })
