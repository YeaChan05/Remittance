package org.yechan.remittance.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test

class IntegrationTestEnvironmentPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "buildlogic.testcontainers-support must be applied to the root project."
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
            val extension = extensions.create("testcontainers", TestcontainersExtension::class.java)
            extensions.add("integrationTestEnvironment", extension)

            tasks.withType(Test::class.java).matching { it.name == "integrationTest" }.configureEach {
                configureIntegrationTestEnvironment(
                    dockerEnvironment,
                    mySqlEnvironment,
                    rabbitMqEnvironment,
                    extension
                )
            }
        }
    }

    private fun Test.configureIntegrationTestEnvironment(
        dockerEnvironmentProvider: Provider<DockerEnvironmentBuildService>,
        mySqlEnvironmentProvider: Provider<MySqlIntegrationTestEnvironmentBuildService>,
        rabbitMqEnvironmentProvider: Provider<RabbitMqIntegrationTestEnvironmentBuildService>,
        extension: TestcontainersExtension
    ) {
        if (extension.resources.isNotEmpty()) {
            usesService(dockerEnvironmentProvider)
        }

        if (TestContainerResource.MYSQL in extension.resources) {
            usesService(mySqlEnvironmentProvider)
        }

        if (TestContainerResource.RABBITMQ in extension.resources) {
            usesService(rabbitMqEnvironmentProvider)
        }

        doFirst {
            if (extension.resources.isEmpty()) {
                return@doFirst
            }

            val coordinates = TestcontainersRuntimeCoordinatesResolver.resolve(project, extension)
            TestcontainersDependencyValidator.validate(project, name, extension, coordinates)

            dockerEnvironmentProvider.get().ensureReady()

            if (TestContainerResource.MYSQL in extension.resources) {
                val mySqlEnvironment = mySqlEnvironmentProvider.get()
                mySqlEnvironment.prepareDatabase(project, path, coordinates)

                systemProperty(SPRING_DATASOURCE_URL, mySqlEnvironment.datasourceUrl(project, path, coordinates))
                systemProperty(SPRING_DATASOURCE_USERNAME, mySqlEnvironment.username(project, coordinates))
                systemProperty(SPRING_DATASOURCE_PASSWORD, mySqlEnvironment.password(project, coordinates))
            }

            if (TestContainerResource.RABBITMQ in extension.resources) {
                val rabbitMqEnvironment = rabbitMqEnvironmentProvider.get()
                rabbitMqEnvironment.ensureReady(project, coordinates)
                systemProperty(SPRING_RABBITMQ_HOST, rabbitMqEnvironment.host(project, coordinates))
                systemProperty(SPRING_RABBITMQ_PORT, rabbitMqEnvironment.port(project, coordinates))
                systemProperty(SPRING_RABBITMQ_USERNAME, rabbitMqEnvironment.username(project, coordinates))
                systemProperty(SPRING_RABBITMQ_PASSWORD, rabbitMqEnvironment.password(project, coordinates))
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
