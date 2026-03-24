package org.yechan.remittance.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project

internal object TestcontainersDependencyValidator {
    fun validate(
        project: Project,
        taskName: String,
        taskSpec: TestcontainersTaskSpec,
        coordinates: TestcontainersRuntimeCoordinates,
        providers: Collection<SharedContainerProvider>,
    ) {
        val validatedModules = buildSet {
            add("testcontainers")
            providers.forEach { addAll(it.validatedModuleNames) }
        }

        val runtimeClasspath =
            project.configurations.findByName("${taskName}RuntimeClasspath") ?: return
        val mismatches = runtimeClasspath.incoming.resolutionResult.allComponents
            .asSequence()
            .mapNotNull { it.moduleVersion }
            .filter { it.group == ORG_TESTCONTAINERS }
            .filterNot { it.version == coordinates.bomVersion }
            .filter { it.name in validatedModules }
            .map { "${it.group}:${it.name}:${it.version}" }
            .distinct()
            .toList()

        if (mismatches.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Testcontainers BOM mismatch detected for ${project.path}:$taskName.")
                    appendLine("Declared BOM: ${coordinates.bomCoordinate}")
                    appendLine("Declared shared containers: ${taskSpec.containerKeys.joinToString(", ")}")
                    appendLine("Resolved modules:")
                    mismatches.forEach { appendLine("- $it") }
                    append("Align the module dependencies with testcontainers { bom(\"${coordinates.bomCoordinate}\") }.")
                },
            )
        }
    }

    private const val ORG_TESTCONTAINERS = "org.testcontainers"
}
