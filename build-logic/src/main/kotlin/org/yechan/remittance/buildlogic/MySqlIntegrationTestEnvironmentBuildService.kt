package org.yechan.remittance.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.net.URLClassLoader
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class MySqlIntegrationTestEnvironmentBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val runtimes = ConcurrentHashMap<TestcontainersRuntimeCoordinates, MySqlRuntime>()

    internal fun prepareDatabase(project: Project, taskPath: String, coordinates: TestcontainersRuntimeCoordinates) {
        runtime(project, coordinates).prepareDatabase(taskPath)
    }

    internal fun datasourceUrl(project: Project, taskPath: String, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).datasourceUrl(taskPath)
    }

    internal fun username(project: Project, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).username()
    }

    internal fun password(project: Project, coordinates: TestcontainersRuntimeCoordinates): String {
        return runtime(project, coordinates).password()
    }

    private fun runtime(project: Project, coordinates: TestcontainersRuntimeCoordinates): MySqlRuntime {
        return runtimes.computeIfAbsent(coordinates) {
            MySqlRuntime(
                TestcontainersRuntimeClasspathResolver.resolve(
                    project = project,
                    coordinates = coordinates,
                    resources = setOf(TestContainerResource.MYSQL)
                )
            )
        }
    }

    override fun close() {
        runtimes.values.forEach(MySqlRuntime::close)
    }

    private class MySqlRuntime(classpath: Set<java.io.File>) : AutoCloseable {
        private val classLoader = URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        private val driver = DriverShim(withContextClassLoader { loadMysqlDriver() })
        private val container = withContextClassLoader { createContainer() }

        init {
            DriverManager.registerDriver(driver)
        }

        fun prepareDatabase(taskPath: String) {
            val databaseName = databaseName(taskPath)
            connection(rootJdbcUrl()).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE DATABASE IF NOT EXISTS `$databaseName`")
                }
            }
        }

        fun datasourceUrl(taskPath: String): String {
            return "jdbc:mysql://${host()}:${mappedPort(MYSQL_PORT)}/${databaseName(taskPath)}?useInformationSchema=true"
        }

        fun username(): String = MYSQL_USERNAME

        fun password(): String = MYSQL_PASSWORD

        private fun createContainer(): Any {
            val dockerImageNameClass = classLoader.loadClass("org.testcontainers.utility.DockerImageName")
            val parse = dockerImageNameClass.getMethod("parse", String::class.java)
            val imageName = parse.invoke(null, "mysql:8.0.36")
            val containerClass = classLoader.loadClass("org.testcontainers.containers.MySQLContainer")
            val container = containerClass.getConstructor(dockerImageNameClass).newInstance(imageName)

            invoke(container, "withDatabaseName", "core")
            invoke(container, "withUsername", MYSQL_USERNAME)
            invoke(container, "withPassword", MYSQL_PASSWORD)
            invoke(container, "withEnv", "MYSQL_ROOT_PASSWORD", MYSQL_PASSWORD)
            invoke(container, "withEnv", "MYSQL_ROOT_HOST", "%")
            invoke(container, "start")
            return container
        }

        private fun connection(url: String): Connection {
            return DriverManager.getConnection(url, MYSQL_USERNAME, MYSQL_PASSWORD)
        }

        private fun loadMysqlDriver(): Driver {
            val driverClass = classLoader.loadClass("com.mysql.cj.jdbc.Driver")
            return driverClass.getDeclaredConstructor().newInstance() as Driver
        }

        private fun rootJdbcUrl(): String {
            return "jdbc:mysql://${host()}:${mappedPort(MYSQL_PORT)}/mysql"
        }

        private fun host(): String = invoke(container, "getHost") as String

        private fun mappedPort(port: Int): Int = invoke(container, "getMappedPort", port) as Int

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

        private fun databaseName(taskPath: String): String {
            return taskPath.trimStart(':')
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .lowercase(Locale.ROOT)
                .take(MAX_DATABASE_NAME_LENGTH)
                .trimEnd('_')
        }

        override fun close() {
            runCatching {
                DriverManager.deregisterDriver(driver)
            }
            runCatching {
                invoke(container, "stop")
            }
            runCatching {
                classLoader.close()
            }
        }
    }

    private class DriverShim(private val delegate: Driver) : Driver by delegate

    private companion object {
        private const val MAX_DATABASE_NAME_LENGTH = 64
        private const val MYSQL_PORT = 3306
        private const val MYSQL_USERNAME = "root"
        private const val MYSQL_PASSWORD = "test"
    }
}
