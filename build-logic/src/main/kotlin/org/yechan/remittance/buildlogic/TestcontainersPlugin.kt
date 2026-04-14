package org.yechan.remittance.buildlogic

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskProvider
import java.util.Locale

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

        project.tasks.register("afterBuild") {
            sharedContainerService.orNull?.close()
        }

        project.gradle.taskGraph.whenReady(
            object : Action<TaskExecutionGraph> {
                override fun execute(taskGraph: TaskExecutionGraph) {
                    selectedTaskPathsByLifecycleOwner(taskRegistrations.values, taskGraph)
                        .forEach { (owner, selectedTaskPaths) ->
                            sharedContainerService.get()
                                .registerExecutionPlan(owner, selectedTaskPaths)
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
                    val executionStackKey = resolveStackKey(currentProject, taskSpec)
                    val taskPath = "${currentProject.path}:${taskSpec.name}"
                    val declaredContainerKeys = taskSpec.containerKeys.toSet()
                    val providerShareScopeKeys = declaredContainerKeys.associateWith { providerKey ->
                        resolveProviderShareScopeKey(
                            projectPath = currentProject.path,
                            taskName = taskSpec.name,
                            executionStackKey = executionStackKey,
                            taskSpec = taskSpec,
                            providerKey = providerKey,
                        )
                    }
                    val coordinates =
                        TestcontainersRuntimeCoordinatesResolver.resolve(currentProject, extension)
                    val runtimeClasspathByProviderKey =
                        SharedContainerRegistry.resolve(declaredContainerKeys)
                            .associate { provider ->
                                provider.key to TestcontainersRuntimeClasspathResolver.resolveConfiguration(
                                    project = currentProject,
                                    coordinates = coordinates,
                                    provider = provider,
                                ).files.toSet()
                            }
                    val taskRegistration = SharedContainerTaskRegistration(
                        projectPath = currentProject.path,
                        taskName = taskSpec.name,
                        executionStackKey = executionStackKey,
                        providerShareScopeKeys = providerShareScopeKeys,
                    )
                    val stackLockService = project.gradle.sharedServices.registerIfAbsent(
                        "sharedContainerStackLockService-$executionStackKey",
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
                            taskRegistration.lifecycleOwners.forEach { owner ->
                                sharedContainerService.get()
                                    .release(owner, taskRegistration.taskPath)
                            }
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
                        taskPath = taskPath,
                        liquibaseChangeLog = taskSpec.liquibaseChangeLog,
                        declaredContainerKeys = declaredContainerKeys,
                        providerShareScopeKeys = providerShareScopeKeys,
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
    val executionStackKey: String,
    val providerShareScopeKeys: Map<String, String>,
) {
    val taskPath: String = "$projectPath:$taskName"
    val lifecycleOwners: Set<SharedContainerLifecycleOwner> = providerShareScopeKeys
        .map { (providerKey, providerShareScopeKey) ->
            SharedContainerLifecycleOwner(providerKey, providerShareScopeKey)
        }
        .toSet()

    fun isRepositoryIntegrationTest(): Boolean = projectPath.endsWith(":repository-jpa") && taskName == INTEGRATION_TEST

    fun isApplicationIntegrationTest(): Boolean = projectPath.endsWith(":application") && taskName == INTEGRATION_TEST

    private companion object {
        private const val INTEGRATION_TEST = "integrationTest"
    }
}

internal fun repositoryBeforeApplicationPairs(
    taskRegistrations: Collection<SharedContainerTaskRegistration>,
): List<Pair<String, String>> = taskRegistrations
    .groupBy(SharedContainerTaskRegistration::executionStackKey)
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

internal const val MYSQL_PROVIDER_KEY = "mysql"
internal const val NON_AGGREGATE_MYSQL_SHARE_SCOPE_KEY = "mysql:non-aggregate"

internal fun resolveProviderShareScopeKey(
    projectPath: String,
    taskName: String,
    executionStackKey: String,
    taskSpec: TestcontainersTaskSpec,
    providerKey: String,
): String {
    val normalizedProviderKey = providerKey.lowercase(Locale.ROOT)
    if (normalizedProviderKey in taskSpec.isolatedContainerKeys) {
        return "$projectPath:$taskName:$normalizedProviderKey:isolate"
    }

    return when {
        normalizedProviderKey == MYSQL_PROVIDER_KEY && projectPath != AGGREGATE_PROJECT_PATH ->
            NON_AGGREGATE_MYSQL_SHARE_SCOPE_KEY
        else -> executionStackKey
    }
}

internal fun selectedTaskPathsByLifecycleOwner(
    taskRegistrations: Collection<SharedContainerTaskRegistration>,
    taskGraph: TaskExecutionGraph,
): Map<SharedContainerLifecycleOwner, Set<String>> = buildMap {
    taskRegistrations
        .filter { taskGraph.hasTask(it.taskPath) }
        .forEach { registration ->
            registration.lifecycleOwners.forEach { owner ->
                put(owner, get(owner).orEmpty() + registration.taskPath)
            }
        }
}

private const val AGGREGATE_PROJECT_PATH = ":aggregate"
