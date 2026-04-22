package org.yechan.remittance.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.yechan.remittance.ApplicationOpenEndpointPolicy
import org.yechan.remittance.ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.OpenEndpointMatcher
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer
import org.yechan.remittance.StaticApplicationOpenEndpointPolicy

@Configuration
class AggregateSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<ApplicationOpenEndpointPolicy> {
            StaticApplicationOpenEndpointPolicy(
                includeHealth = true,
                additionalMatchers =
                listOf(
                    OpenEndpointMatcher(pattern = "/login"),
                    OpenEndpointMatcher(pattern = "/members"),
                    OpenEndpointMatcher(pattern = "/actuator/info"),
                    OpenEndpointMatcher(pattern = "/actuator/prometheus"),
                ),
            )
        }

        registerBean<AuthorizeHttpRequestsCustomizer>("aggregateAuthorizeHttpRequestsCustomizer") {
            PrioritizedAuthorizeHttpRequestsCustomizer(
                Ordered.HIGHEST_PRECEDENCE,
                AuthorizeHttpRequestsCustomizer { registry ->
                    ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer(
                        bean(),
                    ).customize(registry)
                    registry.requestMatchers(HttpMethod.POST, "/login").permitAll()
                    registry.requestMatchers(HttpMethod.POST, "/members").permitAll()
                    registry.requestMatchers("/actuator/info").permitAll()
                    registry.requestMatchers("/actuator/prometheus").permitAll()
                },
            )
        }
    })
