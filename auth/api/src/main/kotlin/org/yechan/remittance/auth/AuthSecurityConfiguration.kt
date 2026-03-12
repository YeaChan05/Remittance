package org.yechan.remittance.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer

@Configuration
class AuthSecurityConfiguration {
    @Bean(name = ["authorizeHttpRequestsCustomizer"])
    fun authorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer {
        return AuthorizeHttpRequestsCustomizer { registry ->
            registry.requestMatchers(HttpMethod.POST, "/login").permitAll()
                .anyRequest().authenticated()
        }
    }
}
