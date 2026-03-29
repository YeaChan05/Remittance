package org.yechan.remittance.buildlogic

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.JavaForkOptions

internal class TaskContainerBinder(
    private val taskProvider: TaskProvider<Task>,
    private val extension: TestcontainersExtension,
    private val taskSpec: TestcontainersTaskSpec,
    private val sharedContainerService: Provider<SharedContainerService>,
    private val stackLockService: Provider<SharedContainerStackLockService>,
    private val stackKey: String,
) {
    fun bind() {
        taskProvider.configure {
            val boundTask = this
            usesService(sharedContainerService)
            usesService(stackLockService)

            doFirst {
                val javaForkTask = boundTask as? JavaForkOptions
                    ?: error("Shared testcontainers support requires JavaForkOptions task: ${boundTask.path}")
                val providers = SharedContainerRegistry.resolve(taskSpec.containerKeys)
                val coordinates =
                    TestcontainersRuntimeCoordinatesResolver.resolve(project, extension)

                TestcontainersDependencyValidator.validate(
                    project = project,
                    taskName = name,
                    taskSpec = taskSpec,
                    coordinates = coordinates,
                    providers = providers,
                )

                val service = sharedContainerService.get()
                service.ensureDockerReady()

                providers.forEach { provider ->
                    service.prepare(project, path, stackKey, coordinates, provider)
                    service.applyTo(javaForkTask, project, path, stackKey, coordinates, provider)
                }
            }
        }
    }
}
