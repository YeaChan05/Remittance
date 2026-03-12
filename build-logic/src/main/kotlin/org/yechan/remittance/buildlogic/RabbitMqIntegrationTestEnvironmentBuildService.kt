package org.yechan.remittance.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

abstract class RabbitMqIntegrationTestEnvironmentBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val runtimes = ConcurrentHashMap<TestcontainersRuntimeCoordinates, RabbitMqRuntime>()

    internal fun ensureReady(project: Project, coordinates: TestcontainersRuntimeCoordinates) {
        runtime(project, coordinates)
    }

    internal fun host(project: Project, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).host()
    }

    internal fun port(project: Project, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).port()
    }

    internal fun username(project: Project, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).username()
    }

    internal fun password(project: Project, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).password()
    }

    private fun runtime(project: Project, coordinates: TestcontainersRuntimeCoordinates): RabbitMqRuntime {
        return runtimes.computeIfAbsent(coordinates) {
            RabbitMqRuntime(
                TestcontainersRuntimeClasspathResolver.resolve(
                    project = project,
                    coordinates = coordinates,
                    resources = setOf(TestContainerResource.RABBITMQ)
                )
            )
        }
    }

    override fun close() {
        runtimes.values.forEach(RabbitMqRuntime::close)
    }

    private class RabbitMqRuntime(classpath: Set<java.io.File>) : AutoCloseable {
        private val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        private val container = withContextClassLoader { createContainer() }

        fun host(): String = invoke(container, "getHost") as String

        fun port(): String = invoke(container, "getAmqpPort").toString()

        fun username(): String = invoke(container, "getAdminUsername") as String

        fun password(): String = invoke(container, "getAdminPassword") as String

        private fun createContainer(): Any {
            val dockerImageNameClass = classLoader.loadClass("org.testcontainers.utility.DockerImageName")
            val parse = dockerImageNameClass.getMethod("parse", String::class.java)
            val imageName = parse.invoke(null, "rabbitmq:3.8.19")
            val containerClass = classLoader.loadClass("org.testcontainers.containers.RabbitMQContainer")
            val container = containerClass.getConstructor(dockerImageNameClass).newInstance(imageName)

            invoke(container, "start")
            return container
        }

        private fun invoke(target: Any, methodName: String, vararg args: Any): Any? {
            return withContextClassLoader {
                val argumentTypes = args.map {
                    when (it) {
                        is Int -> Integer.TYPE
                        else -> it.javaClass
                    }
                }.toTypedArray()
                val method = target.javaClass.getMethod(methodName, *argumentTypes)
                method.invoke(target, *args)
            }
        }

        private fun <T> withContextClassLoader(block: () -> T): T {
            val thread = Thread.currentThread()
            val previous = thread.contextClassLoader
            thread.contextClassLoader = classLoader
            return try {
                block()
            } finally {
                thread.contextClassLoader = previous
            }
        }

        override fun close() {
            runCatching {
                invoke(container, "stop")
            }
            runCatching {
                classLoader.close()
            }
        }
    }
}
