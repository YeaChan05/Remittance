package org.yechan.remittance.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.process.JavaForkOptions
import java.io.File
import java.sql.Connection

internal object MySqlSharedContainerProvider : SharedContainerProvider {
    override val key: String = "mysql"
    override val validatedModuleNames: Set<String> = setOf("mysql", "testcontainers-mysql")

    override fun runtimeDependencies(
        project: Project,
        coordinates: TestcontainersRuntimeCoordinates,
    ): List<Dependency> = listOf(
        project.dependencies.create("org.testcontainers:testcontainers-mysql"),
        project.dependencies.create(
            TestcontainersRuntimeCoordinatesResolver.libraryCoordinate(
                project,
                "mysql-connector-j",
            ),
        ),
    )

    override fun createRuntime(classpath: Set<File>): SharedContainerRuntime = MySqlSharedContainerRuntime(classpath)

    internal fun databaseNameFor(taskPath: String): String = taskPath
        .trimStart(':')
        .replace(':', '_')
        .replace('-', '_')
        .lowercase()
        .take(MAX_DATABASE_NAME_LENGTH)

    private class MySqlSharedContainerRuntime(
        classpath: Set<File>,
    ) : ClasspathBackedSharedContainerRuntime(classpath) {
        private val container = withContextClassLoader { createContainer() }

        override fun prepare(project: Project, taskPath: String) {
            val databaseName = databaseNameFor(taskPath)
            withContextClassLoader {
                createConnection().use { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("CREATE DATABASE IF NOT EXISTS `$databaseName`")
                    }
                }
            }
        }

        override fun applyTo(target: JavaForkOptions, project: Project, taskPath: String) {
            val databaseName = databaseNameFor(taskPath)
            target.systemProperty(SPRING_DATASOURCE_URL, jdbcUrl(databaseName))
            target.systemProperty(SPRING_DATASOURCE_USERNAME, username())
            target.systemProperty(SPRING_DATASOURCE_PASSWORD, password())
            target.systemProperty(SPRING_DATASOURCE_DRIVER_CLASS_NAME, MYSQL_DRIVER_CLASS_NAME)
        }

        private fun jdbcUrl(databaseName: String): String = buildString {
            append("jdbc:mysql://")
            append(host())
            append(':')
            append(port())
            append('/')
            append(databaseName)
            append("?useInformationSchema=true")
        }

        private fun host(): String = invoke(container, "getHost") as String

        private fun port(): String = invoke(container, "getMappedPort", MYSQL_PORT).toString()

        private fun username(): String = invoke(container, "getUsername") as String

        private fun password(): String = invoke(container, "getPassword") as String

        private fun createConnection(): Connection = invoke(container, "createConnection", "") as Connection

        private fun createContainer(): Any {
            val dockerImageNameClass =
                classLoader.loadClass("org.testcontainers.utility.DockerImageName")
            val parse = dockerImageNameClass.getMethod("parse", String::class.java)
            val imageName = parse.invoke(null, "mysql:8.4.8")
            val containerClass =
                classLoader.loadClass("org.testcontainers.containers.MySQLContainer")
            val container =
                containerClass.getConstructor(dockerImageNameClass).newInstance(imageName)

            invoke(container, "withDatabaseName", DEFAULT_DATABASE_NAME)
            invoke(container, "withUsername", DEFAULT_USERNAME)
            invoke(container, "withPassword", DEFAULT_PASSWORD)
            invoke(container, "start")
            return container
        }

        override fun closeRuntime() {
            runCatching {
                invoke(container, "stop")
            }
        }
    }

    private const val DEFAULT_DATABASE_NAME = "test"
    private const val DEFAULT_USERNAME = "root"
    private const val DEFAULT_PASSWORD = "test"
    private const val MYSQL_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver"
    private const val MYSQL_PORT = 3306
    private const val MAX_DATABASE_NAME_LENGTH = 64
    private const val SPRING_DATASOURCE_URL = "spring.datasource.url"
    private const val SPRING_DATASOURCE_USERNAME = "spring.datasource.username"
    private const val SPRING_DATASOURCE_PASSWORD = "spring.datasource.password"
    private const val SPRING_DATASOURCE_DRIVER_CLASS_NAME = "spring.datasource.driver-class-name"
}
