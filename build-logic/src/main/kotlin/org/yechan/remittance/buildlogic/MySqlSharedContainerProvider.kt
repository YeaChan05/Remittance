package org.yechan.remittance.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.testing.Test
import java.sql.Connection
import java.sql.Driver
import java.sql.DriverManager
import java.time.Duration
import java.util.Locale

internal object MySqlSharedContainerProvider : SharedContainerProvider {
    override val key: String = "mysql"
    override val validatedModuleNames: Set<String> = setOf("mysql", "testcontainers-mysql")

    override fun runtimeDependencies(project: Project, coordinates: TestcontainersRuntimeCoordinates): List<Dependency> {
        return listOf(
            project.dependencies.create("org.testcontainers:testcontainers-mysql"),
            project.dependencies.create(TestcontainersRuntimeCoordinatesResolver.libraryCoordinate(project, "mysql-connector-j"))
        )
    }

    override fun createRuntime(classpath: Set<java.io.File>): SharedContainerRuntime {
        return MySqlSharedContainerRuntime(classpath)
    }

    private class MySqlSharedContainerRuntime(
        classpath: Set<java.io.File>
    ) : ClasspathBackedSharedContainerRuntime(classpath) {
        private val driver = DriverShim(withContextClassLoader { loadMysqlDriver() })
        private val container = withContextClassLoader { createContainer() }

        init {
            DriverManager.registerDriver(driver)
        }

        override fun prepare(project: Project, taskPath: String) {
            val databaseName = databaseName(taskPath)
            connection(rootJdbcUrl()).use { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("CREATE DATABASE IF NOT EXISTS `$databaseName`")
                }
            }
        }

        override fun applyTo(testTask: Test, project: Project, taskPath: String) {
            testTask.systemProperty(SPRING_DATASOURCE_URL, datasourceUrl(taskPath))
            testTask.systemProperty(SPRING_DATASOURCE_USERNAME, MYSQL_USERNAME)
            testTask.systemProperty(SPRING_DATASOURCE_PASSWORD, MYSQL_PASSWORD)
        }

        private fun datasourceUrl(taskPath: String): String {
            return "jdbc:mysql://${host()}:${mappedPort(MYSQL_PORT)}/${databaseName(taskPath)}?useInformationSchema=true"
        }

        private fun createContainer(): Any {
            val dockerImageNameClass = classLoader.loadClass("org.testcontainers.utility.DockerImageName")
            val parse = dockerImageNameClass.getMethod("parse", String::class.java)
            val imageName = parse.invoke(null, "mysql:8.4.8")
            val containerClass = classLoader.loadClass("org.testcontainers.containers.MySQLContainer")
            val container = containerClass.getConstructor(dockerImageNameClass).newInstance(imageName)

            invoke(container, "withDatabaseName", "core")
            invoke(container, "withUsername", MYSQL_USERNAME)
            invoke(container, "withPassword", MYSQL_PASSWORD)
            invoke(container, "withEnv", "MYSQL_ROOT_PASSWORD", MYSQL_PASSWORD)
            invoke(container, "withEnv", "MYSQL_ROOT_HOST", "%")
            invoke(container, "withStartupTimeout", Duration.ofMinutes(3))
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

        private fun databaseName(taskPath: String): String {
            return taskPath.trimStart(':')
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .lowercase(Locale.ROOT)
                .take(MAX_DATABASE_NAME_LENGTH)
                .trimEnd('_')
        }

        override fun closeRuntime() {
            runCatching {
                DriverManager.deregisterDriver(driver)
            }
            runCatching {
                invoke(container, "stop")
            }
        }
    }

    private class DriverShim(private val delegate: Driver) : Driver by delegate

    private const val MAX_DATABASE_NAME_LENGTH = 64
    private const val MYSQL_PORT = 3306
    private const val MYSQL_USERNAME = "root"
    private const val MYSQL_PASSWORD = "test"
    private const val SPRING_DATASOURCE_URL = "spring.datasource.url"
    private const val SPRING_DATASOURCE_USERNAME = "spring.datasource.username"
    private const val SPRING_DATASOURCE_PASSWORD = "spring.datasource.password"
}
