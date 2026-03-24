package org.yechan.remittance.auth

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration
class AuthBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AuthLoginUseCase> {
            AuthService(bean(), bean())
        }
    })
