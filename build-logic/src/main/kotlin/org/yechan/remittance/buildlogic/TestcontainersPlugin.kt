package org.yechan.remittance.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

@Suppress("unused")
class TestcontainersPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "buildlogic.testcontainers-support must be applied to the root project."
        }

        val sharedContainerService = project.gradle.sharedServices.registerIfAbsent(
            "sharedContainerBuildService",
            SharedContainerService::class.java,
        ) {}

        project.gradle.buildFinished {
            sharedContainerService.orNull?.close()
        }

        project.allprojects {
            val currentProject = this
            val extension = extensions.create("testcontainers", TestcontainersExtension::class.java)

            afterEvaluate {
                extension.taskSpecs.values.forEach { taskSpec ->
                    require(taskSpec.containerKeys.isNotEmpty()) {
                        "Shared testcontainers must declare at least one container for ${currentProject.path}:${taskSpec.name}."
                    }

                    val taskProvider = tasks.named(taskSpec.name, Task::class.java)
                    val stackKey = resolveStackKey(currentProject, taskSpec)
                    val stackLockService = project.gradle.sharedServices.registerIfAbsent(
                        "sharedContainerStackLockService-$stackKey",
                        SharedContainerStackLockService::class.java,
                    ) {
                        maxParallelUsages.set(1)
                    }
                    TaskContainerBinder(
                        taskProvider = taskProvider,
                        extension = extension,
                        taskSpec = taskSpec,
                        sharedContainerService = sharedContainerService,
                        stackLockService = stackLockService,
                        stackKey = stackKey,
                    ).bind()
                }
            }
        }
    }

    private fun resolveStackKey(
        project: Project,
        taskSpec: TestcontainersTaskSpec,
    ): String = taskSpec.stackKey
        ?: project.findProperty("testcontainers.stack")?.toString()?.trim()?.lowercase()
        ?: project.path.trimStart(':').replace(':', '-').lowercase()
}
