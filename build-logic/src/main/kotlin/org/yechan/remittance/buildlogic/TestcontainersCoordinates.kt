package org.yechan.remittance.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.VersionCatalogsExtension

internal data class TestcontainersRuntimeCoordinates(
    val bomCoordinate: String,
) {
    val bomVersion: String
        get() = bomCoordinate.substringAfterLast(':')
}

internal object TestcontainersRuntimeCoordinatesResolver {
    fun resolve(
        project: Project,
        extension: TestcontainersExtension,
    ): TestcontainersRuntimeCoordinates = TestcontainersRuntimeCoordinates(
        bomCoordinate = extension.bomCoordinate ?: libraryCoordinate(
            project,
            "testcontainers-bom",
        ),
    )

    internal fun libraryCoordinate(project: Project, alias: String): String {
        val libraries =
            project.rootProject.extensions.getByType(VersionCatalogsExtension::class.java)
                .named("libs")
        val dependency = libraries.findLibrary(alias)
            .orElseThrow { GradleException("Version catalog entry '$alias' is required.") }
            .get()
        val version = dependency.versionConstraint.requiredVersion
        return "${dependency.module.group}:${dependency.module.name}:$version"
    }
}

internal object TestcontainersRuntimeClasspathResolver {
    fun resolveConfiguration(
        project: Project,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
    ): Configuration {
        val dependencies = mutableListOf<Dependency>()
        dependencies += project.dependencies.enforcedPlatform(coordinates.bomCoordinate)
        dependencies += project.dependencies.create(TESTCONTAINERS_CORE)
        dependencies += provider.runtimeDependencies(project, coordinates)

        return project.configurations.detachedConfiguration(*dependencies.toTypedArray())
    }

    private const val TESTCONTAINERS_CORE = "org.testcontainers:testcontainers"
}
