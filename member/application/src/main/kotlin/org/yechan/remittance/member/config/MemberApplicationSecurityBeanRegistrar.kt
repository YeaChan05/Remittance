package org.yechan.remittance.member.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

@Configuration
class MemberApplicationSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AuthorizeHttpRequestsCustomizer>("memberApplicationAuthorizeHttpRequestsCustomizer") {
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
                    .requestMatchers(HttpMethod.POST, "/login", "/members").permitAll()
            }
        }
    })
