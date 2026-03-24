package org.yechan.remittance.auth

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

@Configuration
class AuthSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AuthorizeHttpRequestsCustomizer>("authAuthorizeHttpRequestsCustomizer") {
            PrioritizedAuthorizeHttpRequestsCustomizer(
                Ordered.HIGHEST_PRECEDENCE + 10,
            ) { registry ->
                registry.requestMatchers(HttpMethod.POST, "/login").permitAll()
            }
        }
    })
