package org.yechan.remittance

import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.http.HttpMethod

class ApplicationOpenEndpointsCustomizerTest {
    @Test
    fun `swagger openapi 경로는 항상 permit all 처리한다`() {
        val registry = mockApplicationRequestMatcherRegistry()

        applicationOpenEndpointsCustomizer().customize(registry)

        verify(registry).requestMatchers(
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-resources/**",
            "/webjars/**",
            "/swagger/**",
        )
    }

    @Test
    fun `include health 옵션은 actuator health 공개 여부만 제어한다`() {
        val registryWithHealth = mockApplicationRequestMatcherRegistry()
        val registryWithoutHealth = mockApplicationRequestMatcherRegistry()

        applicationOpenEndpointsCustomizer(includeHealth = true).customize(registryWithHealth)
        applicationOpenEndpointsCustomizer(includeHealth = false).customize(registryWithoutHealth)

        verify(registryWithHealth).requestMatchers("/actuator/health")
        verify(registryWithoutHealth, never()).requestMatchers("/actuator/health")
    }

    @Test
    fun `additional matchers는 method 유무에 맞춰 공개 경로를 추가한다`() {
        val registry = mockApplicationRequestMatcherRegistry()

        applicationOpenEndpointsCustomizer(
            additionalMatchers =
                listOf(
                    OpenEndpointMatcher(HttpMethod.POST, "/login"),
                    OpenEndpointMatcher(pattern = "/internal/open"),
                ),
        ).customize(registry)

        verify(registry).requestMatchers(HttpMethod.POST, "/login")
        verify(registry).requestMatchers("/internal/open")
    }

    private fun mockApplicationRequestMatcherRegistry(): ApplicationRequestMatcherRegistry {
        @Suppress("UNCHECKED_CAST")
        return mock(
            ApplicationRequestMatcherRegistry::class.java,
            RETURNS_DEEP_STUBS,
        ) as ApplicationRequestMatcherRegistry
    }
}
