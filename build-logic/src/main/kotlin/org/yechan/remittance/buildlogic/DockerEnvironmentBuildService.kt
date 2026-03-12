package org.yechan.remittance.buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.testcontainers.DockerClientFactory

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
            DockerClientFactory.instance().isDockerAvailable
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
