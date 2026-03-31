package org.yechan.remittance.transfer.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.core.Ordered
import org.yechan.remittance.ApplicationOpenEndpointPolicy
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer

class TransferApplicationSecurityBeanRegistrarTest {
    @Test
    fun `보안 설정은 transfer customizer를 default보다 먼저 등록한다`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(TestConfiguration::class.java)
            refresh()
        }

        val transferCustomizer = context.getBean(
            "transferApplicationAuthorizeHttpRequestsCustomizer",
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

        assertThat(transferCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat(defaultCustomizer).isInstanceOf(PrioritizedAuthorizeHttpRequestsCustomizer::class.java)
        assertThat(policy.includeHealth).isTrue()
        assertThat((transferCustomizer as Ordered).order).isLessThan((defaultCustomizer as Ordered).order)
        assertThat(orderedCustomizers).containsExactly(transferCustomizer, defaultCustomizer)

        context.close()
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TransferApplicationSecurityBeanRegistrar::class)
    class TestConfiguration {
        @Bean("defaultAuthorizeHttpRequestsCustomizer")
        fun defaultAuthorizeHttpRequestsCustomizer(): AuthorizeHttpRequestsCustomizer = PrioritizedAuthorizeHttpRequestsCustomizer(
            Ordered.LOWEST_PRECEDENCE,
        ) { registry -> registry.anyRequest().authenticated() }
    }
}
