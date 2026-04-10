package org.yechan.remittance.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.process.JavaForkOptions
import java.io.File

internal object RabbitMqSharedContainerProvider : SharedContainerProvider {
    override val key: String = "rabbitmq"
    override val validatedModuleNames: Set<String> = setOf("testcontainers-rabbitmq")

    override fun runtimeDependencies(
        project: Project,
        coordinates: TestcontainersRuntimeCoordinates,
    ): List<Dependency> = listOf(
        project.dependencies.create("org.testcontainers:testcontainers-rabbitmq"),
    )

    override fun createRuntime(classpath: Set<File>): SharedContainerRuntime = RabbitMqSharedContainerRuntime(classpath)

    private class RabbitMqSharedContainerRuntime(
        classpath: Set<File>,
    ) : ClasspathBackedSharedContainerRuntime(classpath) {
        private val container = withContextClassLoader { createContainer() }

        override fun applyTo(target: JavaForkOptions, taskPath: String) {
            target.systemProperty(SPRING_RABBITMQ_HOST, host())
            target.systemProperty(SPRING_RABBITMQ_PORT, port())
            target.systemProperty(SPRING_RABBITMQ_USERNAME, username())
            target.systemProperty(SPRING_RABBITMQ_PASSWORD, password())
        }

        private fun host(): String = invoke(container, "getHost") as String

        private fun port(): String = invoke(container, "getAmqpPort").toString()

        private fun username(): String = invoke(container, "getAdminUsername") as String

        private fun password(): String = invoke(container, "getAdminPassword") as String

        private fun createContainer(): Any {
            val dockerImageNameClass =
                classLoader.loadClass("org.testcontainers.utility.DockerImageName")
            val parse = dockerImageNameClass.getMethod("parse", String::class.java)
            val imageName = parse.invoke(null, "rabbitmq:3.8.19")
            val containerClass =
                classLoader.loadClass("org.testcontainers.containers.RabbitMQContainer")
            val container =
                containerClass.getConstructor(dockerImageNameClass).newInstance(imageName)

            invoke(container, "start")
            return container
        }

        override fun closeRuntime() {
            runCatching {
                invoke(container, "stop")
            }
        }
    }

    private const val SPRING_RABBITMQ_HOST = "spring.rabbitmq.host"
    private const val SPRING_RABBITMQ_PORT = "spring.rabbitmq.port"
    private const val SPRING_RABBITMQ_USERNAME = "spring.rabbitmq.username"
    private const val SPRING_RABBITMQ_PASSWORD = "spring.rabbitmq.password"
}
