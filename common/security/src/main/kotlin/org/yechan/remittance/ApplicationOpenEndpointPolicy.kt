package org.yechan.remittance

import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer

data class OpenEndpointMatcher(
    val method: HttpMethod? = null,
    val pattern: String,
)

interface ApplicationOpenEndpointPolicy {
    val includeHealth: Boolean
    val additionalMatchers: List<OpenEndpointMatcher>
}

data class StaticApplicationOpenEndpointPolicy(
    override val includeHealth: Boolean = false,
    override val additionalMatchers: List<OpenEndpointMatcher> = emptyList(),
) : ApplicationOpenEndpointPolicy

class ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer(
    private val policy: ApplicationOpenEndpointPolicy,
) : AuthorizeHttpRequestsCustomizer {
    override fun customize(
        registry: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry,
    ) {
        registry.requestMatchers(
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-resources/**",
            "/webjars/**",
            "/swagger/**",
        ).permitAll()

        if (policy.includeHealth) {
            registry.requestMatchers("/actuator/health").permitAll()
        }

        policy.additionalMatchers.forEach { matcher ->
            when (matcher.method) {
                null -> registry.requestMatchers(matcher.pattern).permitAll()
                else -> registry.requestMatchers(matcher.method, matcher.pattern).permitAll()
            }
        }
    }
}

typealias ApplicationRequestMatcherRegistry =
    AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
