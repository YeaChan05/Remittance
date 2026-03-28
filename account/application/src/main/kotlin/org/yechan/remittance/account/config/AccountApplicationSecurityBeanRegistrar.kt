package org.yechan.remittance.account.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

@Configuration
class AccountApplicationSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AuthorizeHttpRequestsCustomizer>("accountApplicationAuthorizeHttpRequestsCustomizer") {
            PrioritizedAuthorizeHttpRequestsCustomizer(
                Ordered.HIGHEST_PRECEDENCE,
            ) { registry ->
                registry
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/swagger/**",
                        "/actuator/health",
                    ).permitAll()
            }
        }
    })
