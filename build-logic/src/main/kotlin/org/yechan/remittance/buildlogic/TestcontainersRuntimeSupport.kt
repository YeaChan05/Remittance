package org.yechan.remittance.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.VersionCatalogsExtension

internal data class TestcontainersRuntimeCoordinates(
    val bomCoordinate: String,
    val mySqlConnectorCoordinate: String
) {
    val bomVersion: String
        get() = bomCoordinate.substringAfterLast(':')
}

internal object TestcontainersRuntimeCoordinatesResolver {
    fun resolve(project: Project, extension: TestcontainersExtension): TestcontainersRuntimeCoordinates {
        return TestcontainersRuntimeCoordinates(
            bomCoordinate = extension.bomCoordinate ?: libraryCoordinate(project, "testcontainers-bom"),
            mySqlConnectorCoordinate = libraryCoordinate(project, "mysql-connector-j")
        )
    }

    private fun libraryCoordinate(project: Project, alias: String): String {
        val libraries = project.rootProject.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val dependency = libraries.findLibrary(alias)
            .orElseThrow { GradleException("Version catalog entry '$alias' is required.") }
            .get()
        val version = dependency.versionConstraint.requiredVersion
        return "${dependency.module.group}:${dependency.module.name}:$version"
    }
}

internal object TestcontainersRuntimeClasspathResolver {
    fun resolve(
        project: Project,
        coordinates: TestcontainersRuntimeCoordinates,
        resources: Set<TestContainerResource>
    ): Set<java.io.File> {
        val dependencies = mutableListOf<Dependency>()
        dependencies += project.dependencies.enforcedPlatform(coordinates.bomCoordinate)
        dependencies += project.dependencies.create(TESTCONTAINERS_CORE)

        if (TestContainerResource.MYSQL in resources) {
            dependencies += project.dependencies.create(TESTCONTAINERS_MYSQL)
            dependencies += project.dependencies.create(coordinates.mySqlConnectorCoordinate)
        }

        if (TestContainerResource.RABBITMQ in resources) {
            dependencies += project.dependencies.create(TESTCONTAINERS_RABBITMQ)
        }

        return project.configurations.detachedConfiguration(*dependencies.toTypedArray()).resolve()
    }

    private const val TESTCONTAINERS_CORE = "org.testcontainers:testcontainers"
    private const val TESTCONTAINERS_MYSQL = "org.testcontainers:testcontainers-mysql"
    private const val TESTCONTAINERS_RABBITMQ = "org.testcontainers:testcontainers-rabbitmq"
}

internal object TestcontainersDependencyValidator {
    fun validate(project: Project, taskName: String, extension: TestcontainersExtension, coordinates: TestcontainersRuntimeCoordinates) {
        if (extension.bomCoordinate == null) {
            return
        }

        val runtimeClasspath = project.configurations.findByName("${taskName}RuntimeClasspath") ?: return
        val mismatches = runtimeClasspath.incoming.resolutionResult.allComponents
            .mapNotNull { it.moduleVersion }
            .filter { it.group == ORG_TESTCONTAINERS }
            .filterNot { it.version == coordinates.bomVersion }
            .filter { shouldValidate(it.name, extension.resources) }
            .map { "${it.group}:${it.name}:${it.version}" }
            .distinct()

        if (mismatches.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Testcontainers BOM mismatch detected for ${project.path}:$taskName.")
                    appendLine("Declared BOM: ${coordinates.bomCoordinate}")
                    appendLine("Resolved modules:")
                    mismatches.forEach { appendLine("- $it") }
                    append("Align the module dependencies with testcontainers { bom(\"${coordinates.bomCoordinate}\") }.")
                }
            )
        }
    }

    private fun shouldValidate(moduleName: String, resources: Set<TestContainerResource>): Boolean {
        return when (moduleName) {
            "testcontainers" -> true
            "mysql", "testcontainers-mysql" -> TestContainerResource.MYSQL in resources
            "rabbitmq", "testcontainers-rabbitmq" -> TestContainerResource.RABBITMQ in resources
            else -> false
        }
    }

    private const val ORG_TESTCONTAINERS = "org.testcontainers"
}
