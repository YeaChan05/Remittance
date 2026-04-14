package org.yechan.remittance.buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.JavaForkOptions
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal data class SharedContainerRuntimeKey(
    val providerKey: String,
    val stackKey: String,
    val coordinates: TestcontainersRuntimeCoordinates,
)

abstract class SharedContainerService :
    BuildService<BuildServiceParameters.None>,
    AutoCloseable {
    private val runtimes = ConcurrentHashMap<SharedContainerRuntimeKey, SharedContainerRuntime>()
    private val stackTracker = SharedContainerStackTracker()
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
        stackKey: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
        runtimeClasspath: Set<File>,
    ) {
        runtime(stackKey, coordinates, provider, runtimeClasspath).prepare(taskPath)
    }

    internal fun applyTo(
        target: JavaForkOptions,
        taskPath: String,
        stackKey: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
        runtimeClasspath: Set<File>,
    ) {
        runtime(stackKey, coordinates, provider, runtimeClasspath).applyTo(target, taskPath)
    }

    internal fun registerExecutionPlan(
        stackKey: String,
        taskPaths: Set<String>,
    ) {
        if (taskPaths.isEmpty()) {
            return
        }

        synchronized(this) {
            stackTracker.register(stackKey, taskPaths)
        }
    }

    internal fun release(
        stackKey: String,
        taskPath: String,
    ) {
        val shouldClose = synchronized(this) {
            stackTracker.release(stackKey, taskPath)
        }

        if (shouldClose) {
            closeStack(stackKey)
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
        stackKey: String,
        coordinates: TestcontainersRuntimeCoordinates,
        provider: SharedContainerProvider,
        runtimeClasspath: Set<File>,
    ): SharedContainerRuntime {
        val key = SharedContainerRuntimeKey(provider.key, stackKey, coordinates)
        return runtimes.computeIfAbsent(key) {
            provider.createRuntime(runtimeClasspath)
        }
    }

    private fun closeStack(stackKey: String) {
        runtimes.keys
            .filter { it.stackKey == stackKey }
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

internal class SharedContainerStackTracker {
    private val expectedTaskPathsByStack = linkedMapOf<String, Set<String>>()
    private val completedTaskPathsByStack = linkedMapOf<String, MutableSet<String>>()

    fun register(
        stackKey: String,
        taskPaths: Set<String>,
    ) {
        expectedTaskPathsByStack[stackKey] = taskPaths.toSet()
        completedTaskPathsByStack.remove(stackKey)
    }

    fun release(
        stackKey: String,
        taskPath: String,
    ): Boolean {
        val expectedTaskPaths = expectedTaskPathsByStack[stackKey] ?: return false
        if (taskPath !in expectedTaskPaths) {
            return false
        }

        val completedTaskPaths = completedTaskPathsByStack.getOrPut(stackKey) {
            linkedSetOf()
        }
        completedTaskPaths += taskPath

        if (!completedTaskPaths.containsAll(expectedTaskPaths)) {
            return false
        }

        expectedTaskPathsByStack.remove(stackKey)
        completedTaskPathsByStack.remove(stackKey)
        return true
    }
}
