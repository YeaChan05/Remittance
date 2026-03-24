package org.yechan.remittance.auth

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration
class AuthClientBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<MemberAuthClient> {
            MemberAuthClientAdapter(bean())
        }
    })
