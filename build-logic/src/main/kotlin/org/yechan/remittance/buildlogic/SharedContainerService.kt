package org.yechan.remittance.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.testing.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal data class SharedContainerRuntimeKey(
    val providerKey: String,
    val coordinates: TestcontainersRuntimeCoordinates
)

abstract class SharedContainerService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val runtimes = ConcurrentHashMap<SharedContainerRuntimeKey, SharedContainerRuntime>()
    private val dockerReadyInitialized = AtomicBoolean(false)
    private val dockerReady = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    internal fun ensureDockerReady() {
        check(isDockerReady()) {
            "Docker environment is not available."
        }
    }

    internal fun prepare(
        project: Project,
        taskPath: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider
    ) {
        runtime(project, coordinates, provider).prepare(project, taskPath)
    }

    internal fun applyTo(
        testTask: Test,
        project: Project,
        taskPath: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider
    ) {
        runtime(project, coordinates, provider).applyTo(testTask, project, taskPath)
    }

    private fun isDockerReady(): Boolean {
        if (dockerReadyInitialized.get()) {
            return dockerReady.get()
        }

        synchronized(this) {
            if (dockerReadyInitialized.get()) {
                return dockerReady.get()
            }

            dockerReady.set(
                runCatching {
                    val process = ProcessBuilder("docker", "info")
                        .redirectErrorStream(true)
                        .start()
                    process.inputStream.bufferedReader().use { it.readText() }
                    process.waitFor() == 0
                }.getOrDefault(false)
            )
            dockerReadyInitialized.set(true)
        }

        return dockerReady.get()
    }

    private fun runtime(
        project: Project,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider
    ): SharedContainerRuntime {
        val key = SharedContainerRuntimeKey(provider.key, coordinates)
        return runtimes.computeIfAbsent(key) {
            provider.createRuntime(
                TestcontainersRuntimeClasspathResolver.resolve(
                    project = project,
                    coordinates = coordinates,
                    provider = provider
                )
            )
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }

        runtimes.values.forEach(SharedContainerRuntime::close)
        runtimes.clear()
    }
}
