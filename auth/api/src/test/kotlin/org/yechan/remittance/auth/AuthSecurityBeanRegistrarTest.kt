package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

class AuthSecurityBeanRegistrarTest {
    @Test
    fun `보안 설정은 auth customizer를 default보다 먼저 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(TestConfiguration::class.java)
            refresh()
        }

        val authCustomizer = context.getBean(
            "authAuthorizeHttpRequestsCustomizer",
            AuthorizeHttpRequestsCustomizer::class.java,
        )
        val defaultCustomizer = context.getBean(
            "defaultAuthorizeHttpRequestsCustomizer",
            AuthorizeHttpRequestsCustomizer::class.java,
        )
        val orderedCustomizers =
            context.getBeanProvider(AuthorizeHttpRequestsCustomizer::class.java)
                .orderedStream()
                .toList()

        assertThat(authCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat(defaultCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat((authCustomizer as Ordered).order).isLessThan((defaultCustomizer as Ordered).order)
        assertThat(orderedCustomizers).containsExactly(authCustomizer, defaultCustomizer)

        @Suppress("UNCHECKED_CAST")
        val registry =
            mock(
                AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry::class.java,
                RETURNS_DEEP_STUBS,
            ) as AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
        authCustomizer.customize(registry)
        verify(registry).requestMatchers(HttpMethod.POST, "/login")

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AuthSecurityBeanRegistrar::class)
    class TestConfiguration {
        @Bean("defaultAuthorizeHttpRequestsCustomizer")
        fun defaultAuthorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer = PrioritizedAuthorizeHttpRequestsCustomizer(
            Ordered.LOWEST_PRECEDENCE,
        ) { registry -> registry.anyRequest().authenticated() }
    }
}
