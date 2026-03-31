package org.yechan.remittance.member.config

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
class MemberApplicationSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<ApplicationOpenEndpointPolicy> {
            StaticApplicationOpenEndpointPolicy(
                includeHealth = true,
                additionalMatchers =
                    listOf(
                        OpenEndpointMatcher(HttpMethod.POST, "/login"),
                        OpenEndpointMatcher(HttpMethod.POST, "/members"),
                    ),
            )
        }

        registerBean<AuthorizeHttpRequestsCustomizer>("memberApplicationAuthorizeHttpRequestsCustomizer") {
            PrioritizedAuthorizeHttpRequestsCustomizer(
                Ordered.HIGHEST_PRECEDENCE,
                ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer(
                    bean<ApplicationOpenEndpointPolicy>(),
                ),
            )
        }
    })
