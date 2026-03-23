package org.yechan.remittance.auth

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

class AuthSecurityConfigurationTest {
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
        val orderedCustomizers = context.getBeanProvider(AuthorizeHttpRequestsCustomizer::class.java)
            .orderedStream()
            .toList()

        assertThat(authCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat(defaultCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat((authCustomizer as Ordered).order).isLessThan((defaultCustomizer as Ordered).order)
        assertThat(orderedCustomizers).containsExactly(authCustomizer, defaultCustomizer)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AuthSecurityConfiguration::class)
    class TestConfiguration {
        @Bean("defaultAuthorizeHttpRequestsCustomizer")
        fun defaultAuthorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer = PrioritizedAuthorizeHttpRequestsCustomizer(
            Ordered.LOWEST_PRECEDENCE,
        ) { registry -> registry.anyRequest().authenticated() }
    }
}
