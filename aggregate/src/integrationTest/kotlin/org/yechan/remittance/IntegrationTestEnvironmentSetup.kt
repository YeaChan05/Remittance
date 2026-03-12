package org.yechan.remittance

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

abstract class IntegrationTestEnvironmentSetup {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerIntegrationTestEnvironmentProperties(registry: DynamicPropertyRegistry) {
            IntegrationTestEnvironmentSystemProperties.registerDataSourceProperties(registry)
            IntegrationTestEnvironmentSystemProperties.registerRabbitProperties(registry)
        }
    }
}
