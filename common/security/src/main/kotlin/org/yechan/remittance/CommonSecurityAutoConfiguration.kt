package org.yechan.remittance

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@AutoConfiguration(before = [ServletWebSecurityAutoConfiguration::class])
@EnableConfigurationProperties(AuthTokenProperties::class)
class CommonSecurityAutoConfiguration {
    @Bean
    fun tokenGenerator(authTokenProperties: AuthTokenProperties): TokenGenerator {
        return JwtTokenGenerator(
            authTokenProperties.salt,
            authTokenProperties.accessExpiresIn,
            authTokenProperties.refreshExpiresIn
        )
    }

    @Bean
    fun tokenParser(): TokenParser {
        return JwtTokenParser()
    }

    @Bean
    fun tokenVerifier(authTokenProperties: AuthTokenProperties): TokenVerifier {
        return JwtTokenVerifier(authTokenProperties.salt)
    }

    @Bean
    fun jwtAuthenticationFilter(
        parser: TokenParser,
        verifier: TokenVerifier,
        authenticationEntryPoint: AuthenticationEntryPoint
    ): JwtAuthenticationFilter {
        return JwtAuthenticationFilter(parser, verifier, authenticationEntryPoint)
    }

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint {
        return DefaultAuthenticationEntryPoint()
    }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler {
        return DefaultAccessDeniedHandler()
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        authenticationEntryPoint: AuthenticationEntryPoint,
        accessDeniedHandler: AccessDeniedHandler,
        @Qualifier("authorizeHttpRequestsCustomizer")
        authorizeHttpRequestsCustomizer: AuthorizeHttpRequestsCustomizer
    ): SecurityFilterChain {
        return http
            .formLogin(FormLoginConfigurer<HttpSecurity>::disable)
            .csrf(CsrfConfigurer<HttpSecurity>::disable)
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .exceptionHandling { handler ->
                handler.authenticationEntryPoint(authenticationEntryPoint)
                handler.accessDeniedHandler(accessDeniedHandler)
            }
            .authorizeHttpRequests(authorizeHttpRequestsCustomizer::customize)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean(name = ["authorizeHttpRequestsCustomizer"])
    @ConditionalOnMissingBean(name = ["authorizeHttpRequestsCustomizer"])
    fun authorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer {
        return AuthorizeHttpRequestsCustomizer { registry -> registry.anyRequest().authenticated() }
    }
}
