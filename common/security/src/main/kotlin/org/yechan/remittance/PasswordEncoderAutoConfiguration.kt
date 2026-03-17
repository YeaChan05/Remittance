package org.yechan.remittance

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Import(PasswordEncoderBeanRegistrar::class)
@AutoConfiguration
class PasswordEncoderAutoConfiguration

class PasswordEncoderBeanRegistrar : BeanRegistrarDsl({
    registerBean<PasswordEncoder> {
        BCryptPasswordEncoder()
    }

    registerBean<PasswordHashEncoder> {
        BcryptPasswordHashEncoder(bean())
    }
})
