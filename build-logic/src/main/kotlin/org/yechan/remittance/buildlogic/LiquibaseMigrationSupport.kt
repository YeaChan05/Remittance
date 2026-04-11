package org.yechan.remittance.buildlogic

import java.io.File
import java.net.URLClassLoader
import java.sql.Connection
import java.util.Properties

internal object LiquibaseMigrationSupport {
    private const val DATASOURCE_URL = "spring.datasource.url"
    private const val DATASOURCE_USERNAME = "spring.datasource.username"
    private const val DATASOURCE_PASSWORD = "spring.datasource.password"
    private const val DATASOURCE_DRIVER = "spring.datasource.driver-class-name"
    private const val SPRING_LIQUIBASE_ENABLED = "spring.liquibase.enabled"

    fun migrate(
        classpath: Set<File>,
        systemProperties: Map<String, Any>,
        changeLog: String,
    ) {
        val jdbcUrl = systemProperties[DATASOURCE_URL]?.toString()
            ?: error("Liquibase migration requires '$DATASOURCE_URL'.")
        val username = systemProperties[DATASOURCE_USERNAME]?.toString()
            ?: error("Liquibase migration requires '$DATASOURCE_USERNAME'.")
        val password = systemProperties[DATASOURCE_PASSWORD]?.toString()
            ?: error("Liquibase migration requires '$DATASOURCE_PASSWORD'.")
        val driverClassName = systemProperties[DATASOURCE_DRIVER]?.toString()

        URLClassLoader(
            classpath.map { it.toURI().toURL() }.toTypedArray(),
            javaClass.classLoader,
        ).use { classLoader ->
            withContextClassLoader(classLoader) {
                openConnection(
                    classLoader = classLoader,
                    driverClassName = driverClassName,
                    jdbcUrl = jdbcUrl,
                    username = username,
                    password = password,
                ).use { connection ->
                    val jdbcConnectionClass = classLoader.loadClass("liquibase.database.jvm.JdbcConnection")
                    val databaseConnectionClass = classLoader.loadClass("liquibase.database.DatabaseConnection")
                    val jdbcConnection = jdbcConnectionClass
                        .getConstructor(java.sql.Connection::class.java)
                        .newInstance(connection)

                    val databaseFactoryClass = classLoader.loadClass("liquibase.database.DatabaseFactory")
                    val databaseFactory = databaseFactoryClass.getMethod("getInstance").invoke(null)
                    val database = databaseFactoryClass
                        .getMethod("findCorrectDatabaseImplementation", databaseConnectionClass)
                        .invoke(databaseFactory, jdbcConnection)

                    val resourceAccessorClass = classLoader.loadClass("liquibase.resource.ResourceAccessor")
                    val classLoaderResourceAccessorClass = classLoader.loadClass("liquibase.resource.ClassLoaderResourceAccessor")
                    val resourceAccessor = classLoaderResourceAccessorClass
                        .getConstructor(ClassLoader::class.java)
                        .newInstance(classLoader)

                    val databaseClass = classLoader.loadClass("liquibase.database.Database")
                    val liquibaseClass = classLoader.loadClass("liquibase.Liquibase")
                    val liquibase = liquibaseClass
                        .getConstructor(String::class.java, resourceAccessorClass, databaseClass)
                        .newInstance(changeLog.removePrefix("classpath:/"), resourceAccessor, database)

                    val contextsClass = classLoader.loadClass("liquibase.Contexts")
                    val contexts = contextsClass.getConstructor().newInstance()
                    val labelExpressionClass = classLoader.loadClass("liquibase.LabelExpression")
                    val labelExpression = labelExpressionClass.getConstructor().newInstance()

                    try {
                        liquibaseClass.getMethod("update", contextsClass, labelExpressionClass)
                            .invoke(liquibase, contexts, labelExpression)
                    } finally {
                        runCatching {
                            databaseClass.getMethod("close").invoke(database)
                        }
                    }
                }
            }
        }
    }

    fun disableSpringLiquibase(systemProperties: MutableMap<String, Any>) {
        systemProperties[SPRING_LIQUIBASE_ENABLED] = "false"
    }

    private fun openConnection(
        classLoader: ClassLoader,
        driverClassName: String?,
        jdbcUrl: String,
        username: String,
        password: String,
    ): Connection {
        val driver = requireNotNull(driverClassName) {
            "Liquibase migration requires '$DATASOURCE_DRIVER'."
        }.let { className ->
            val driverClass = Class.forName(className, true, classLoader)
            driverClass.getDeclaredConstructor().newInstance() as java.sql.Driver
        }

        val properties = Properties().apply {
            put("user", username)
            put("password", password)
        }

        return requireNotNull(driver.connect(jdbcUrl, properties)) {
            "Driver $driverClassName could not connect to $jdbcUrl"
        }
    }

    private fun <T> withContextClassLoader(
        classLoader: ClassLoader,
        block: () -> T,
    ): T {
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = classLoader
        return try {
            block()
        } finally {
            thread.contextClassLoader = previous
        }
    }
}
