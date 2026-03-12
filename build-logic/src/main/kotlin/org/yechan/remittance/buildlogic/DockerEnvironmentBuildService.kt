package org.yechan.remittance.buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class DockerEnvironmentBuildService : BuildService<BuildServiceParameters.None> {
    @Volatile
    private var initialized: Boolean = false

    @Volatile
    private var available: Boolean = false

    @Synchronized
    fun isAvailable(): Boolean {
        if (initialized) {
            return available
        }

        available = runCatching {
            val process = ProcessBuilder("docker", "info")
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() == 0
        }.getOrDefault(false)
        initialized = true
        return available
    }

    @Synchronized
    fun ensureReady() {
        check(isAvailable()) {
            "Docker environment is not available."
        }
    }
}
