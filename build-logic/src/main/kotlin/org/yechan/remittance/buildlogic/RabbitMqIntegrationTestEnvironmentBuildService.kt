package org.yechan.remittance.buildlogic

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.utility.DockerImageName

abstract class RabbitMqIntegrationTestEnvironmentBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    @Volatile
    private var container: RabbitMQContainer? = null

    @Synchronized
    fun ensureReady() {
        container()
    }

    fun host(): String = container().host

    fun port(): String = container().amqpPort.toString()

    fun username(): String = container().adminUsername

    fun password(): String = container().adminPassword

    @Synchronized
    private fun container(): RabbitMQContainer {
        container?.let {
            return it
        }

        return RabbitMQContainer(DockerImageName.parse("rabbitmq:3.8.19")).also {
            it.start()
            container = it
        }
    }

    override fun close() {
        container?.stop()
    }
}
