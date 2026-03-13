package org.yechan.remittance.buildlogic

import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test

internal class TaskContainerBinder(
    private val testTask: TaskProvider<Test>,
    private val extension: TestcontainersExtension,
    private val taskSpec: TestcontainersTaskSpec,
    private val sharedContainerService: Provider<SharedContainerService>
) {
    fun bind() {
        testTask.configure {
            val boundTestTask = this
            usesService(sharedContainerService)

            doFirst {
                val providers = SharedContainerRegistry.resolve(taskSpec.containerKeys)
                val coordinates = TestcontainersRuntimeCoordinatesResolver.resolve(project, extension)

                TestcontainersDependencyValidator.validate(
                    project = project,
                    taskName = name,
                    taskSpec = taskSpec,
                    coordinates = coordinates,
                    providers = providers
                )

                val service = sharedContainerService.get()
                service.ensureDockerReady()

                providers.forEach { provider ->
                    service.prepare(project, path, coordinates, provider)
                    service.applyTo(boundTestTask, project, path, coordinates, provider)
                }
            }
        }
    }
}
