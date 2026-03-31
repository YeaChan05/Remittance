package org.yechan.remittance

import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer

data class OpenEndpointMatcher(
    val method: HttpMethod? = null,
    val pattern: String,
)

private val applicationOpenEndpointPatterns =
    arrayOf(
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/v3/api-docs.yaml",
        "/swagger-resources/**",
        "/webjars/**",
        "/swagger/**",
    )

fun applicationOpenEndpointsCustomizer(
    includeHealth: Boolean = false,
    additionalMatchers: List<OpenEndpointMatcher> = emptyList(),
): AuthorizeHttpRequestsCustomizer = AuthorizeHttpRequestsCustomizer { registry ->
    registry.requestMatchers(*applicationOpenEndpointPatterns).permitAll()

    if (includeHealth) {
        registry.requestMatchers("/actuator/health").permitAll()
    }

    additionalMatchers.forEach { matcher ->
        when (matcher.method) {
            null -> registry.requestMatchers(matcher.pattern).permitAll()
            else -> registry.requestMatchers(matcher.method, matcher.pattern).permitAll()
        }
    }
}

typealias ApplicationRequestMatcherRegistry =
    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
