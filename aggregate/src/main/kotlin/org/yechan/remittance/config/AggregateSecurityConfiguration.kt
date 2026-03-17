package org.yechan.remittance.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer

@Import(AggregateSecurityBeanRegistrar::class)
@Configuration
class AggregateSecurityConfiguration

class AggregateSecurityBeanRegistrar : BeanRegistrarDsl({
    registerBean<AuthorizeHttpRequestsCustomizer> {
        AuthorizeHttpRequestsCustomizer { registry ->
            registry
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/swagger/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/login", "/members").permitAll()
                .anyRequest().authenticated()
        }
    }
})
