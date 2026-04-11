package org.yechan.remittance.buildlogic

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskProvider
import java.util.concurrent.ConcurrentHashMap

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
        val taskProvidersByPath = linkedMapOf<String, TaskProvider<Task>>()
        val releaseTaskProvidersByPath = linkedMapOf<String, TaskProvider<DefaultTask>>()
        val taskRegistrations = linkedMapOf<String, SharedContainerTaskRegistration>()
        val selectedTaskPathsByStack = ConcurrentHashMap<String, Set<String>>()

        project.tasks.register("afterBuild") {
            sharedContainerService.orNull?.close()
        }

        project.gradle.taskGraph.whenReady(
            object : Action<TaskExecutionGraph> {
                override fun execute(taskGraph: TaskExecutionGraph) {
                    taskRegistrations.values
                        .groupBy(SharedContainerTaskRegistration::stackKey)
                        .forEach { (stackKey, registrations) ->
                            val selectedTaskPaths = registrations
                                .map(SharedContainerTaskRegistration::taskPath)
                                .filter(taskGraph::hasTask)
                                .toSet()

                            if (selectedTaskPaths.isEmpty()) {
                                return@forEach
                            }

                            selectedTaskPathsByStack[stackKey] = selectedTaskPaths
                            sharedContainerService.get()
                                .registerExecutionPlan(stackKey, selectedTaskPaths)
                        }
                }
            },
        )
        project.gradle.projectsEvaluated {
            repositoryBeforeApplicationPairs(taskRegistrations.values).forEach { (repositoryTaskPath, applicationTaskPath) ->
                val repositoryTask = taskProvidersByPath[repositoryTaskPath] ?: return@forEach
                val repositoryReleaseTask =
                    releaseTaskProvidersByPath[repositoryTaskPath] ?: return@forEach
                val applicationTask = taskProvidersByPath[applicationTaskPath] ?: return@forEach
                applicationTask.configure {
                    mustRunAfter(repositoryTask, repositoryReleaseTask)
                }
            }
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
                    val taskPath = "${currentProject.path}:${taskSpec.name}"
                    val declaredContainerKeys = taskSpec.containerKeys.toSet()
                    val coordinates = TestcontainersRuntimeCoordinatesResolver.resolve(currentProject, extension)
                    val runtimeClasspathByProviderKey =
                        SharedContainerRegistry.resolve(declaredContainerKeys).associate { provider ->
                            provider.key to TestcontainersRuntimeClasspathResolver.resolveConfiguration(
                                project = currentProject,
                                coordinates = coordinates,
                                provider = provider,
                            ).files.toSet()
                        }
                    val taskRegistration = SharedContainerTaskRegistration(
                        projectPath = currentProject.path,
                        taskName = taskSpec.name,
                        stackKey = stackKey,
                    )
                    val stackLockService = project.gradle.sharedServices.registerIfAbsent(
                        "sharedContainerStackLockService-$stackKey",
                        SharedContainerStackLockService::class.java,
                    ) {
                        maxParallelUsages.set(1)
                    }
                    val releaseTaskProvider = tasks.register(
                        "${taskSpec.name}ReleaseSharedContainers",
                        DefaultTask::class.java,
                    ) {
                        usesService(sharedContainerService)
                        usesService(stackLockService)
                        doLast {
                            sharedContainerService.get()
                                .release(stackKey, taskRegistration.taskPath)
                        }
                    }
                    taskProvidersByPath[taskRegistration.taskPath] = taskProvider
                    releaseTaskProvidersByPath[taskRegistration.taskPath] = releaseTaskProvider
                    taskRegistrations[taskRegistration.taskPath] = taskRegistration
                    taskProvider.configure {
                        finalizedBy(releaseTaskProvider)
                    }
                    bindSharedContainers(
                        taskProvider = taskProvider,
                        sharedContainerService = sharedContainerService,
                        stackLockService = stackLockService,
                        stackKey = stackKey,
                        taskPath = taskPath,
                        liquibaseChangeLog = taskSpec.liquibaseChangeLog,
                        declaredContainerKeys = declaredContainerKeys,
                        coordinates = coordinates,
                        runtimeClasspathByProviderKey = runtimeClasspathByProviderKey,
                    )
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

internal data class SharedContainerTaskRegistration(
    val projectPath: String,
    val taskName: String,
    val stackKey: String,
) {
    val taskPath: String = "$projectPath:$taskName"

    fun isRepositoryIntegrationTest(): Boolean = projectPath.endsWith(":repository-jpa") && taskName == INTEGRATION_TEST

    fun isApplicationIntegrationTest(): Boolean = projectPath.endsWith(":application") && taskName == INTEGRATION_TEST

    private companion object {
        private const val INTEGRATION_TEST = "integrationTest"
    }
}

internal fun repositoryBeforeApplicationPairs(
    taskRegistrations: Collection<SharedContainerTaskRegistration>,
): List<Pair<String, String>> = taskRegistrations
    .groupBy(SharedContainerTaskRegistration::stackKey)
    .values
    .flatMap { registrations ->
        val repositoryTasks =
            registrations.filter(SharedContainerTaskRegistration::isRepositoryIntegrationTest)
        val applicationTasks =
            registrations.filter(SharedContainerTaskRegistration::isApplicationIntegrationTest)

        applicationTasks.flatMap { applicationTask ->
            repositoryTasks.map { repositoryTask ->
                repositoryTask.taskPath to applicationTask.taskPath
            }
        }
    }
