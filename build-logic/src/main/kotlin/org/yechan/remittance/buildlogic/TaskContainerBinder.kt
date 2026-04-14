package org.yechan.remittance.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions
import java.io.File

internal fun bindSharedContainers(
    taskProvider: TaskProvider<Task>,
    sharedContainerService: Provider<SharedContainerService>,
    stackLockService: Provider<SharedContainerStackLockService>,
    taskPath: String,
    liquibaseChangeLog: String?,
    declaredContainerKeys: Set<String>,
    providerShareScopeKeys: Map<String, String>,
    coordinates: TestcontainersRuntimeCoordinates,
    runtimeClasspathByProviderKey: Map<String, Set<File>>,
) {
    taskProvider.configure {
        usesService(sharedContainerService)
        usesService(stackLockService)

        doFirst {
            val javaForkTask = this as? JavaForkOptions
                ?: error("Shared testcontainers support requires JavaForkOptions task: $taskPath")
            val testTask = this as? Test
                ?: error("Shared testcontainers support requires Test task: $taskPath")
            val providers = SharedContainerRegistry.resolve(declaredContainerKeys)
            TestcontainersDependencyValidator.findMismatchMessage(
                taskPath = taskPath,
                declaredContainerKeys = declaredContainerKeys,
                coordinates = coordinates,
                providers = providers,
                runtimeClasspathFiles = testTask.classpath.files,
            )?.let(::GradleException)?.let { throw it }

            val service = sharedContainerService.get()
            service.ensureDockerReady()

            providers.forEach { provider ->
                val runtimeClasspath = runtimeClasspathByProviderKey.getValue(provider.key)
                val providerShareScopeKey = providerShareScopeKeys.getValue(provider.key)
                service.prepare(taskPath, providerShareScopeKey, coordinates, provider, runtimeClasspath)
                service.applyTo(
                    javaForkTask,
                    taskPath,
                    providerShareScopeKey,
                    coordinates,
                    provider,
                    runtimeClasspath,
                )
            }

            liquibaseChangeLog?.let { changeLog ->
                val systemProperties = javaForkTask.systemProperties
                    .mapValues { (_, value) -> value ?: "" }
                    .toMutableMap()
                LiquibaseMigrationSupport.migrate(
                    classpath = testTask.classpath.files,
                    systemProperties = systemProperties,
                    changeLog = changeLog,
                )
                LiquibaseMigrationSupport.disableSpringLiquibase(systemProperties)
                javaForkTask.systemProperties(systemProperties)
            }
        }
    }
}
