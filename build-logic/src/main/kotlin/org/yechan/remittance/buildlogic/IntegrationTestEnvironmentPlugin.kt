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

        project.allprojects {
            tasks.withType(Test::class.java).configureEach {
                when (path) {
                    ":account:repository-jpa:integrationTest",
                    ":member:repository-jpa:integrationTest",
                    ":transfer:repository-jpa:integrationTest" -> {
                        configureMySqlIntegrationTest(
                            dockerEnvironment,
                            registerMySqlEnvironment(project, path)
                        )
                    }

                    ":aggregate:integrationTest" -> {
                        configureMySqlIntegrationTest(
                            dockerEnvironment,
                            registerMySqlEnvironment(project, path),
                            registerRabbitMqEnvironment(project, path)
                        )
                    }
                }
            }
        }
    }

    private fun registerMySqlEnvironment(
        project: Project,
        taskPath: String
    ): Provider<MySqlIntegrationTestEnvironmentBuildService> {
        return project.gradle.sharedServices.registerIfAbsent(
            serviceName(taskPath, "mysql"),
            MySqlIntegrationTestEnvironmentBuildService::class.java
        ) {}
    }

    private fun registerRabbitMqEnvironment(
        project: Project,
        taskPath: String
    ): Provider<RabbitMqIntegrationTestEnvironmentBuildService> {
        return project.gradle.sharedServices.registerIfAbsent(
            serviceName(taskPath, "rabbitmq"),
            RabbitMqIntegrationTestEnvironmentBuildService::class.java
        ) {}
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
            mySqlEnvironment.ensureReady()

            systemProperty(SPRING_DATASOURCE_URL, mySqlEnvironment.datasourceUrl())
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

    private fun serviceName(taskPath: String, environmentName: String): String {
        val normalizedTaskPath = taskPath.trimStart(':').replace(':', '-')
        return "${normalizedTaskPath}-${environmentName}-integration-test-environment"
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
