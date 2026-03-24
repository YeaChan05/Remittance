package org.yechan.remittance.buildlogic

import java.util.Locale

open class TestcontainersExtension {
    internal val taskSpecs: MutableMap<String, TestcontainersTaskSpec> = linkedMapOf()
    internal var bomCoordinate: String? = null

    fun task(name: String, action: TestcontainersTaskSpec.() -> Unit) {
        require(name.isNotBlank()) {
            "Shared testcontainer task name must not be blank."
        }

        val normalizedTaskName = name.trim()
        val spec = taskSpecs.getOrPut(normalizedTaskName) {
            TestcontainersTaskSpec(normalizedTaskName)
        }
        spec.action()
    }

    fun bom(coordinate: String) {
        bomCoordinate = coordinate
    }
}

open class TestcontainersTaskSpec internal constructor(
    internal val name: String,
) {
    internal val containerKeys: MutableSet<String> = linkedSetOf()

    fun use(containerKey: String) {
        require(containerKey.isNotBlank()) {
            "Shared testcontainer key must not be blank."
        }

        containerKeys += containerKey.lowercase(Locale.ROOT)
    }

    @Deprecated("Use use(\"mysql\") instead.")
    fun mysql() {
        use("mysql")
    }

    @Deprecated("Use use(\"rabbitmq\") instead.")
    fun rabbitMq() {
        use("rabbitmq")
    }
}
