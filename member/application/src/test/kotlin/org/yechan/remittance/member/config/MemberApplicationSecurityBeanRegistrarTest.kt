package org.yechan.remittance.member.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEEP_STUBS
import org.mockito.Mockito.mock
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.yechan.remittance.ApplicationOpenEndpointPolicy
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

class MemberApplicationSecurityBeanRegistrarTest {
    @Test
    fun `보안 설정은 member customizer를 default보다 먼저 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(TestConfiguration::class.java)
            refresh()
        }

        val memberCustomizer = context.getBean(
            "memberApplicationAuthorizeHttpRequestsCustomizer",
            AuthorizeHttpRequestsCustomizer::class.java,
        )
        val defaultCustomizer = context.getBean(
            "defaultAuthorizeHttpRequestsCustomizer",
            AuthorizeHttpRequestsCustomizer::class.java,
        )
        val policy = context.getBean(ApplicationOpenEndpointPolicy::class.java)
        val orderedCustomizers =
            context.getBeanProvider(AuthorizeHttpRequestsCustomizer::class.java)
                .orderedStream()
                .toList()

        assertThat(memberCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat(defaultCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat(policy.includeHealth).isTrue()
        assertThat((memberCustomizer as Ordered).order).isLessThan((defaultCustomizer as Ordered).order)
        assertThat(orderedCustomizers).containsExactly(memberCustomizer, defaultCustomizer)

        @Suppress("UNCHECKED_CAST")
        val registry =
            mock(
                AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry::class.java,
                RETURNS_DEEP_STUBS,
            ) as AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
        memberCustomizer.customize(registry)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(MemberApplicationSecurityBeanRegistrar::class)
    class TestConfiguration {
        @Bean("defaultAuthorizeHttpRequestsCustomizer")
        fun defaultAuthorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer = PrioritizedAuthorizeHttpRequestsCustomizer(
            Ordered.LOWEST_PRECEDENCE,
        ) { registry -> registry.anyRequest().authenticated() }
    }
}
