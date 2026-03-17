package org.yechan.remittance.auth

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer

@Import(AuthSecurityBeanRegistrar::class)
@Configuration
class AuthSecurityConfiguration

class AuthSecurityBeanRegistrar : BeanRegistrarDsl({
    registerBean<AuthorizeHttpRequestsCustomizer> {
        AuthorizeHttpRequestsCustomizer { registry ->
            registry.requestMatchers(HttpMethod.POST, "/login").permitAll()
                .anyRequest().authenticated()
        }
    }
})
