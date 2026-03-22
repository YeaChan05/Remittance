package org.yechan.remittance

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Import(CommonSecurityBeanRegistrar::class)
@AutoConfiguration
@EnableConfigurationProperties(AuthTokenProperties::class)
class CommonSecurityAutoConfiguration

class CommonSecurityBeanRegistrar : BeanRegistrarDsl({
    registerBean<TokenGenerator> {
        val authTokenProperties = bean<AuthTokenProperties>()

        JwtTokenGenerator(
            authTokenProperties.salt,
            authTokenProperties.accessExpiresIn,
            authTokenProperties.refreshExpiresIn
        )
    }

    registerBean<TokenParser> {
        JwtTokenParser()
    }

    registerBean<TokenVerifier> {
        JwtTokenVerifier(bean<AuthTokenProperties>().salt)
    }

    registerBean<AuthenticationEntryPoint> {
        DefaultAuthenticationEntryPoint()
    }

    registerBean<AccessDeniedHandler> {
        DefaultAccessDeniedHandler()
    }

    registerBean<JwtAuthenticationFilter> {
        JwtAuthenticationFilter(
            bean(),
            bean(),
            bean()
        )
    }

    registerBean<AuthorizeHttpRequestsCustomizer>("defaultAuthorizeHttpRequestsCustomizer") {
        PrioritizedAuthorizeHttpRequestsCustomizer(
            Ordered.LOWEST_PRECEDENCE
        ) { registry -> registry.anyRequest().authenticated() }
    }

    registerBean<SecurityFilterChain> {
        val authorizeHttpRequestsCustomizers = beanProvider<AuthorizeHttpRequestsCustomizer>()

        bean<HttpSecurity>()
            .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
            .csrf(CsrfConfigurer<HttpSecurity>::disable)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { handler ->
                handler.authenticationEntryPoint(bean())
                handler.accessDeniedHandler(bean())
            }
            .authorizeHttpRequests { registry ->
                authorizeHttpRequestsCustomizers.orderedStream().forEach { customizer ->
                    customizer.customize(registry)
                }
            }
            .addFilterBefore(
                bean<JwtAuthenticationFilter>(),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()
    }
})
