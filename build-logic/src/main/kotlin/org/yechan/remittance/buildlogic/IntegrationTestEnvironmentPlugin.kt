package org.yechan.remittance.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

class IntegrationTestEnvironmentPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "remittance.integration-test-environment must be applied to the root project."
        }

        val dockerEnvironment = project.gradle.sharedServices.registerIfAbsent(
            "dockerEnvironmentBuildService",
            DockerEnvironmentBuildService::class.java
        ) {}
        val mySqlEnvironment = project.gradle.sharedServices.registerIfAbsent(
            "sharedMySqlIntegrationTestEnvironmentBuildService",
            MySqlIntegrationTestEnvironmentBuildService::class.java
        ) {}
        val rabbitMqEnvironment = project.gradle.sharedServices.registerIfAbsent(
            "sharedRabbitMqIntegrationTestEnvironmentBuildService",
            RabbitMqIntegrationTestEnvironmentBuildService::class.java
        ) {}

        project.allprojects {
            tasks.withType(Test::class.java).configureEach {
                when (path) {
                    ":account:repository-jpa:integrationTest",
                    ":member:repository-jpa:integrationTest",
                    ":transfer:repository-jpa:integrationTest" -> {
                        configureMySqlIntegrationTest(
                            dockerEnvironment,
                            mySqlEnvironment
                        )
                    }

                    ":aggregate:integrationTest" -> {
                        configureMySqlIntegrationTest(
                            dockerEnvironment,
                            mySqlEnvironment,
                            rabbitMqEnvironment
                        )
                    }
                }
            }
        }
    }

    private fun Test.configureMySqlIntegrationTest(
        dockerEnvironmentProvider: Provider<DockerEnvironmentBuildService>,
        mySqlEnvironmentProvider: Provider<MySqlIntegrationTestEnvironmentBuildService>,
        rabbitMqEnvironmentProvider: Provider<RabbitMqIntegrationTestEnvironmentBuildService>? = null
    ) {
        usesService(dockerEnvironmentProvider)
        usesService(mySqlEnvironmentProvider)
        rabbitMqEnvironmentProvider?.let(::usesService)

        doFirst {
            dockerEnvironmentProvider.get().ensureReady()

            val mySqlEnvironment = mySqlEnvironmentProvider.get()
            mySqlEnvironment.prepareDatabase(path)

            systemProperty(SPRING_DATASOURCE_URL, mySqlEnvironment.datasourceUrl(path))
            systemProperty(SPRING_DATASOURCE_USERNAME, mySqlEnvironment.username())
            systemProperty(SPRING_DATASOURCE_PASSWORD, mySqlEnvironment.password())

            rabbitMqEnvironmentProvider?.get()?.let { rabbitMqEnvironment ->
                rabbitMqEnvironment.ensureReady()
                systemProperty(SPRING_RABBITMQ_HOST, rabbitMqEnvironment.host())
                systemProperty(SPRING_RABBITMQ_PORT, rabbitMqEnvironment.port())
                systemProperty(SPRING_RABBITMQ_USERNAME, rabbitMqEnvironment.username())
                systemProperty(SPRING_RABBITMQ_PASSWORD, rabbitMqEnvironment.password())
            }
        }
    }

    private companion object {
        private const val SPRING_DATASOURCE_URL = "spring.datasource.url"
        private const val SPRING_DATASOURCE_USERNAME = "spring.datasource.username"
        private const val SPRING_DATASOURCE_PASSWORD = "spring.datasource.password"
        private const val SPRING_RABBITMQ_HOST = "spring.rabbitmq.host"
        private const val SPRING_RABBITMQ_PORT = "spring.rabbitmq.port"
        private const val SPRING_RABBITMQ_USERNAME = "spring.rabbitmq.username"
        private const val SPRING_RABBITMQ_PASSWORD = "spring.rabbitmq.password"
    }
}
