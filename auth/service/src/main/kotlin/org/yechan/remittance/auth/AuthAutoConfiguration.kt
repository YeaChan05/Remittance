package org.yechan.remittance.auth

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@Import(AuthBeanRegistrar::class)
@AutoConfiguration
class AuthAutoConfiguration

class AuthBeanRegistrar : BeanRegistrarDsl({
    registerBean<AuthLoginUseCase> {
        AuthService(bean(), bean())
    }
})
