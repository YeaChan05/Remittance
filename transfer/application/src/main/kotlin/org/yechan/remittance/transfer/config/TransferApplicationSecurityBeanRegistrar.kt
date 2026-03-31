package org.yechan.remittance.transfer.config

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.yechan.remittance.ApplicationOpenEndpointPolicy
import org.yechan.remittance.ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer
import org.yechan.remittance.AuthorizeHttpRequestsCustomizer
import org.yechan.remittance.PrioritizedAuthorizeHttpRequestsCustomizer
import org.yechan.remittance.StaticApplicationOpenEndpointPolicy

@Configuration
class TransferApplicationSecurityBeanRegistrar :
    BeanRegistrarDsl({
        registerBean<ApplicationOpenEndpointPolicy> {
            StaticApplicationOpenEndpointPolicy(includeHealth = true)
        }

        registerBean<AuthorizeHttpRequestsCustomizer>("transferApplicationAuthorizeHttpRequestsCustomizer") {
            PrioritizedAuthorizeHttpRequestsCustomizer(
                Ordered.HIGHEST_PRECEDENCE,
                ApplicationOpenEndpointsAuthorizeHttpRequestsCustomizer(
                    bean(),
                ),
            )
        }
    })
