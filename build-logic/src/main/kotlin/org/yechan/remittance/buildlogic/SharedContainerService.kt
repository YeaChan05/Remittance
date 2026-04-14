package org.yechan.remittance.buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.JavaForkOptions
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal data class SharedContainerRuntimeKey(
    val providerKey: String,
    val providerShareScopeKey: String,
    val coordinates: TestcontainersRuntimeCoordinates,
)

internal data class SharedContainerLifecycleOwner(
    val providerKey: String,
    val providerShareScopeKey: String,
)

abstract class SharedContainerService :
    BuildService<BuildServiceParameters.None>,
    AutoCloseable {
    private val runtimes = ConcurrentHashMap<SharedContainerRuntimeKey, SharedContainerRuntime>()
    private val scopeTracker = SharedContainerScopeTracker()
    private val dockerReadyInitialized = AtomicBoolean(false)
    private val dockerReady = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    internal fun ensureDockerReady() {
        check(isDockerReady()) {
            "Docker environment is not available."
        }
    }

    internal fun prepare(
        taskPath: String,
        providerShareScopeKey: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
        runtimeClasspath: Set<File>,
    ) {
        runtime(providerShareScopeKey, coordinates, provider, runtimeClasspath).prepare(taskPath)
    }

    internal fun applyTo(
        target: JavaForkOptions,
        taskPath: String,
        providerShareScopeKey: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
        runtimeClasspath: Set<File>,
    ) {
        runtime(providerShareScopeKey, coordinates, provider, runtimeClasspath).applyTo(target, taskPath)
    }

    internal fun registerExecutionPlan(
        owner: SharedContainerLifecycleOwner,
        taskPaths: Set<String>,
    ) {
        if (taskPaths.isEmpty()) {
            return
        }

        synchronized(this) {
            scopeTracker.register(owner, taskPaths)
        }
    }

    internal fun release(
        owner: SharedContainerLifecycleOwner,
        taskPath: String,
    ) {
        val shouldClose = synchronized(this) {
            scopeTracker.release(owner, taskPath)
        }

        if (shouldClose) {
            closeScope(owner)
        }
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
                }.getOrDefault(false),
            )
            dockerReadyInitialized.set(true)
        }

        return dockerReady.get()
    }

    private fun runtime(
        providerShareScopeKey: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
        runtimeClasspath: Set<File>,
    ): SharedContainerRuntime {
        val key = SharedContainerRuntimeKey(provider.key, providerShareScopeKey, coordinates)
        return runtimes.computeIfAbsent(key) {
            provider.createRuntime(runtimeClasspath)
        }
    }

    private fun closeScope(owner: SharedContainerLifecycleOwner) {
        runtimes.keys
            .filter {
                it.providerKey == owner.providerKey &&
                    it.providerShareScopeKey == owner.providerShareScopeKey
            }
            .forEach { key ->
                runtimes.remove(key)?.close()
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

abstract class SharedContainerStackLockService :
    BuildService<BuildServiceParameters.None>,
    AutoCloseable {
    override fun close() = Unit
}

internal class SharedContainerScopeTracker {
    private val expectedTaskPathsByOwner = linkedMapOf<SharedContainerLifecycleOwner, Set<String>>()
    private val completedTaskPathsByOwner = linkedMapOf<SharedContainerLifecycleOwner, MutableSet<String>>()

    fun register(
        owner: SharedContainerLifecycleOwner,
        taskPaths: Set<String>,
    ) {
        expectedTaskPathsByOwner[owner] = taskPaths.toSet()
        completedTaskPathsByOwner.remove(owner)
    }

    fun release(
        owner: SharedContainerLifecycleOwner,
        taskPath: String,
    ): Boolean {
        val expectedTaskPaths = expectedTaskPathsByOwner[owner] ?: return false
        if (taskPath !in expectedTaskPaths) {
            return false
        }

        val completedTaskPaths = completedTaskPathsByOwner.getOrPut(owner) {
            linkedSetOf()
        }
        completedTaskPaths += taskPath

        if (!completedTaskPaths.containsAll(expectedTaskPaths)) {
            return false
        }

        expectedTaskPathsByOwner.remove(owner)
        completedTaskPathsByOwner.remove(owner)
        return true
    }
}
