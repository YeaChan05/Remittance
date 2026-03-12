package org.yechan.remittance

import org.springframework.test.context.DynamicPropertyRegistry

object IntegrationTestEnvironmentSystemProperties {
    const val SPRING_DATASOURCE_URL = "spring.datasource.url"
    const val SPRING_DATASOURCE_USERNAME = "spring.datasource.username"
    const val SPRING_DATASOURCE_PASSWORD = "spring.datasource.password"
    const val SPRING_RABBITMQ_HOST = "spring.rabbitmq.host"
    const val SPRING_RABBITMQ_PORT = "spring.rabbitmq.port"
    const val SPRING_RABBITMQ_USERNAME = "spring.rabbitmq.username"
    const val SPRING_RABBITMQ_PASSWORD = "spring.rabbitmq.password"

    fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
        registry.add(SPRING_DATASOURCE_URL) { requiredProperty(SPRING_DATASOURCE_URL) }
        registry.add(SPRING_DATASOURCE_USERNAME) { requiredProperty(SPRING_DATASOURCE_USERNAME) }
        registry.add(SPRING_DATASOURCE_PASSWORD) { requiredProperty(SPRING_DATASOURCE_PASSWORD) }
    }

    fun registerRabbitProperties(registry: DynamicPropertyRegistry) {
        registry.add(SPRING_RABBITMQ_HOST) { requiredProperty(SPRING_RABBITMQ_HOST) }
        registry.add(SPRING_RABBITMQ_PORT) { requiredProperty(SPRING_RABBITMQ_PORT) }
        registry.add(SPRING_RABBITMQ_USERNAME) { requiredProperty(SPRING_RABBITMQ_USERNAME) }
        registry.add(SPRING_RABBITMQ_PASSWORD) { requiredProperty(SPRING_RABBITMQ_PASSWORD) }
    }

    private fun requiredProperty(propertyName: String): String {
        return System.getProperty(propertyName)
            ?: throw IllegalStateException("Missing Gradle-provided system property: $propertyName")
    }
}
