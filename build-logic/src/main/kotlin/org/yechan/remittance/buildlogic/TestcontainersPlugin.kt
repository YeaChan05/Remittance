package org.yechan.remittance.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

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

                    val testTask = tasks.named(taskSpec.name, Test::class.java)
                    TaskContainerBinder(
                        testTask = testTask,
                        extension = extension,
                        taskSpec = taskSpec,
                        sharedContainerService = sharedContainerService,
                    ).bind()
                }
            }
        }
    }
}
