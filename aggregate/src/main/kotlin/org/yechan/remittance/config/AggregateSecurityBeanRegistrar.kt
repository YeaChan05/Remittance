package org.yechan.remittance.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.OpenEndpointMatcher
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer
import org.yechan.remittance.applicationOpenEndpointsCustomizer

@Configuration
class AggregateSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<AuthorizeHttpRequestsCustomizer>("aggregateAuthorizeHttpRequestsCustomizer") {
            PrioritizedAuthorizeHttpRequestsCustomizer(
                Ordered.HIGHEST_PRECEDENCE,
                applicationOpenEndpointsCustomizer(
                    additionalMatchers =
                    listOf(
                        OpenEndpointMatcher(HttpMethod.POST, "/login"),
                        OpenEndpointMatcher(HttpMethod.POST, "/members"),
                    ),
                ),
            )
        }
    })
