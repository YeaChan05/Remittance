package org.yechan.remittance.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer

@Configuration
class AggregateSecurityConfiguration {
    @Bean("authorizeHttpRequestsCustomizer")
    fun authorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer {
        return AuthorizeHttpRequestsCustomizer { registry ->
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
}
