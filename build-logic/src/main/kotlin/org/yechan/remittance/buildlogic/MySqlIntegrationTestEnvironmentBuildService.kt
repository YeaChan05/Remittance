package org.yechan.remittance.buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

abstract class MySqlIntegrationTestEnvironmentBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    @Volatile
    private var container: MySQLContainer<*>? = null

    @Synchronized
    fun ensureReady() {
        val container = container()
        Class.forName("com.mysql.cj.jdbc.Driver")

        DriverManager.getConnection(rootJdbcUrl(container), MYSQL_USERNAME, MYSQL_PASSWORD).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE DATABASE IF NOT EXISTS core")
                statement.execute("CREATE DATABASE IF NOT EXISTS integration")
            }
        }
    }

    fun datasourceUrl(): String {
        val container = container()
        return "jdbc:mysql://${container.host}:${container.getMappedPort(MYSQL_PORT)}/core?useInformationSchema=true"
    }

    fun username(): String = MYSQL_USERNAME

    fun password(): String = MYSQL_PASSWORD

    @Synchronized
    private fun container(): MySQLContainer<*> {
        container?.let {
            return it
        }

        return MySQLContainer(DockerImageName.parse("mysql:8.0.36"))
            .withDatabaseName("core")
            .withUsername(MYSQL_USERNAME)
            .withPassword(MYSQL_PASSWORD)
            .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_PASSWORD)
            .withEnv("MYSQL_ROOT_HOST", "%")
            .also {
                it.start()
                container = it
            }
    }

    private fun rootJdbcUrl(container: MySQLContainer<*>): String {
        return "jdbc:mysql://${container.host}:${container.getMappedPort(MYSQL_PORT)}/mysql"
    }

    override fun close() {
        container?.stop()
    }

    private companion object {
        private const val MYSQL_PORT = 3306
        private const val MYSQL_USERNAME = "root"
        private const val MYSQL_PASSWORD = "test"
    }
}
