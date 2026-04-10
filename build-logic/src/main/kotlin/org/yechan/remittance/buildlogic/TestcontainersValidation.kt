package org.yechan.remittance.buildlogic

import java.io.File

internal object TestcontainersDependencyValidator {
    fun findMismatchMessage(
        taskPath: String,
        declaredContainerKeys: Collection<String>,
        coordinates: TestcontainersRuntimeCoordinates,
        providers: Collection<SharedContainerProvider>,
        runtimeClasspathFiles: Set<File>,
    ): String? {
        val validatedModules = buildSet {
            add("testcontainers")
            providers.forEach { addAll(it.validatedModuleNames) }
        }

        val moduleNames = validatedModules.sortedByDescending(String::length)
        val mismatches = runtimeClasspathFiles
            .asSequence()
            .mapNotNull { file ->
                val name = file.name.removeSuffix(".jar")
                val module =
                    moduleNames.firstOrNull { candidate ->
                        name.startsWith("$candidate-")
                    } ?: return@mapNotNull null
                val version = name.removePrefix("$module-")
                if (version.isEmpty() || !version.first().isDigit()) {
                    return@mapNotNull null
                }
                if (version == coordinates.bomVersion) {
                    return@mapNotNull null
                }
                "org.testcontainers:$module:$version"
            }
            .distinct()
            .toList()

        if (mismatches.isEmpty()) {
            return null
        }

        return buildString {
            appendLine("Testcontainers BOM mismatch detected for $taskPath.")
            appendLine("Declared BOM: ${coordinates.bomCoordinate}")
            appendLine("Declared shared containers: ${declaredContainerKeys.joinToString(", ")}")
            appendLine("Resolved modules:")
            mismatches.forEach { appendLine("- $it") }
            append("Align the module dependencies with testcontainers { bom(\"${coordinates.bomCoordinate}\") }.")
        }
    }
}
