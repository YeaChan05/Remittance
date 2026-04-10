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
    stackKey: String,
    taskPath: String,
    declaredContainerKeys: Set<String>,
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
                service.prepare(taskPath, stackKey, coordinates, provider, runtimeClasspath)
                service.applyTo(javaForkTask, taskPath, stackKey, coordinates, provider, runtimeClasspath)
            }
        }
    }
}
