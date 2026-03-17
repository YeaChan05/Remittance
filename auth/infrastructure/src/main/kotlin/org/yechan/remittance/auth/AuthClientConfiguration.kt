package org.yechan.remittance.auth

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Import

@Import(AuthClientBeanRegistrar::class)
@AutoConfiguration
class AuthClientConfiguration

class AuthClientBeanRegistrar : BeanRegistrarDsl({
    registerBean<MemberAuthClient> {
        MemberAuthClientAdapter(bean())
    }
})
